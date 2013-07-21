package mdesl.font;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

import mdesl.font.BitmapFontWriter.FontInfo;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.PixmapPacker.Page;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.filters.FreeTypeFilter;
import com.badlogic.gdx.graphics.g2d.freetype.filters.FreeTypePaddingFilter;
import com.badlogic.gdx.graphics.g2d.freetype.filters.FreeTypeShadowFilter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.SharedLibraryLoader;

public class FontPackTool {

	public static final String DEFAULT_CHARS = FreeTypeFontGenerator.DEFAULT_CHARS;
	public static final String ABRIDGED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
															+ "abcdefghijklmnopqrstuvwxyz\n1234567890" 
															+ "\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*";
	
	public static class Settings {
		public int[] sizes;
		
		public boolean flip = false;
		
		public int paddingLeft, paddingTop, paddingRight, paddingBottom;
		
		public boolean glow = false;
		public int glowOffsetX;
		public int glowOffsetY;
		public int glowBlurRadius;
		public int glowBlurIterations;
		public Color glowColor = new Color(0f, 0f, 0f, 1f);
		
		public String characters = DEFAULT_CHARS;
	}
	
	public static class FontItem {
		public String key;
		public String name;
		public String path;
		public Settings settings;
		
		public FontItem() { 
		}
		
		public FontItem(String key, String path, String name, Settings settings) {
			this.key = key;
			this.path = path;
			this.name = name;
			this.settings = settings;
		}
		
		public FontItem(String key, String path) {
			this(key, path, null, null);
		}
		
		public String toString() {
			return (name!=null ? name : key) + " (" + path + ")";
		}
	}
	
	public static class FontPackDocument {
		public int atlasWidth = 512;
		public int atlasHeight = 512;
		public int spacing = 2;
		public Settings defaultSettings = new Settings();
		public Array<FontItem> fonts = new Array<FontItem>();
	}
	
	public static class FontData {
		public BitmapFontData data;
		public Settings settings;
		public FontItem item;
	}
	
	public static class FontPack {
		public PixmapPacker atlas;
		public ObjectMap<String, IntMap<FontData>> dataMap;
	}
	
	private static Json json = new Json();
	private static ProgressListener listener;
	private static int totalGlyphs = 0;
	private static int loadedGlyphs = 0;
	
	public static FontPackDocument read(URL url) throws IOException {
		InputStream in = url.openStream();
		FontPackDocument pack = json.fromJson(FontPackDocument.class, in);
		in.close();
		return pack;
	}
	
	public static FontPackDocument read(File f) throws IOException {
		FileReader re = new FileReader(f);
		FontPackDocument pack = json.fromJson(FontPackDocument.class, re);
		re.close();
		return pack;
	}
	
	public static void write(URL url, FontPackDocument doc) throws IOException {
		OutputStreamWriter wr = new OutputStreamWriter(url.openConnection().getOutputStream());
		json.toJson(doc, FontPackDocument.class, wr);
		wr.close();
	}
	
	public static void write(File f, FontPackDocument doc) throws IOException {
		FileWriter wr = new FileWriter(f);
		json.toJson(doc, FontPackDocument.class, wr);
		wr.close();
	}
	
	public static void setListener(ProgressListener pl) {
		listener = pl;
	}
	
	public static ProgressListener getListener() {
		return listener;
	}
	
	public static void main(String[] args) throws IOException {
		new SharedLibraryLoader("libs/gdx-natives.jar").load("gdx");
		new SharedLibraryLoader("libs/gdx-freetype-natives.jar").load("gdx-freetype");
		
		FontPackDocument doc = null;
		String outDir = "out";
		String outName = "fonts";
		if (args!=null && args.length>0) {
			File f = new File(args[0]);
			if (f.exists())
				doc = read(f);
			else
				throw new IOException("Could not find '"+f.getAbsolutePath()+"'");
			
			if (args.length>1)
				outDir = args[1];
			if (args.length>2)
				outName = args[2];
		} else
			doc = new FontPackDocument();
		
		if (doc.fonts==null || doc.fonts.size==0) {
			System.out.println("No fonts to parse -- exiting");
			System.exit(0);
		}
		
		FontPack packer = pack(doc, new FileHandle(outDir), outName);
		packer.atlas.dispose();
	}
	
	static void setupFilters(FreeTypeFontGenerator gen, Settings s) {
		gen.getFilters().clear();
		
		gen.addFilter(new ProgressFilter());
		
		if (s.paddingLeft!=0 || s.paddingTop!=0 || s.paddingRight!=0 || s.paddingBottom!=0) {
			gen.addFilter(new FreeTypePaddingFilter(s.paddingLeft, s.paddingTop, s.paddingRight, s.paddingBottom));
		}
		
		if (s.glow) {
			Color col = new Color(s.glowColor);
			gen.addFilter( new FreeTypeShadowFilter(col, s.glowOffsetX, s.glowOffsetY,
										s.glowBlurRadius, s.glowBlurIterations, 0) );
		} 	
	}
	
	public static FontPack pack(FontPackDocument doc, FileHandle outDir, String imageOutName) throws IOException {
		PixmapPacker packer = new PixmapPacker(doc.atlasWidth, doc.atlasHeight, Format.RGBA8888, doc.spacing, false);
		
		Settings defSettings = doc.defaultSettings;
		if (defSettings==null)
			throw new IOException("default settings can't be null");
		
		ObjectMap<String, IntMap<FontData>> dataMap = new ObjectMap<String, IntMap<FontData>>();
		
		//currently loaded font count
		int loadedFontCount = 0;
		
		totalGlyphs = 0;
		int totalFonts = 0;
		
		System.out.println("Generating data...");
		
		for (FontItem font : doc.fonts){
			Settings s = font.settings!=null ? font.settings : defSettings;
			int[] sizes = s.sizes != null && s.sizes.length != 0 ? s.sizes : defSettings.sizes; 
			
			if (sizes==null||sizes.length==0) {
				System.out.println("\tInfo: No size array exists for "+font.key+", ignoring");
				continue;
			}
			for (int size : sizes) {
				totalGlyphs += s.characters.length();
				totalFonts++;
			}
		}
		System.out.println();
		
		for (FontItem font : doc.fonts) {
			Settings s = font.settings!=null ? font.settings : defSettings;
			int[] sizes = s.sizes != null && s.sizes.length != 0 ? s.sizes : defSettings.sizes; 
			
			if (sizes==null || sizes.length==0)
				continue;
			
			if (font.key==null||font.key.length()==0)
				throw new IOException("font key must not be empty/null");
			
			//initialize the new font...
			FileHandle file = new FileHandle(font.path);
			if (!file.exists())
				throw new IOException(font.key+" does not exist at path "+font.path);
			
			FreeTypeFontGenerator gen = null;
			try {
				gen = new FreeTypeFontGenerator(file);
			} catch (Exception e) {
				throw new InvalidFontFileException(font);
			}
			setupFilters(gen, s);
			
			for (int i=0; i<sizes.length; i++) {
				//reset the glyph progress to zero
				loadedGlyphs = 0;
				
				IntMap<FontData> oldMap = dataMap.get(font.key);
				//check for any key collisions
				if (oldMap!=null) {
					FontData oldData = oldMap.get(sizes[i]);
					if (oldData!=null) 
						throw new IOException("duplicate font with the key '"+font.key+"'");
				}
				
				//generate the data, packing it into our atlas
				BitmapFontData data = null;
				try {
					data = gen.generateData(sizes[i], s.characters, s.flip, packer);
				} catch (GdxRuntimeException e) {
					throw new InvalidFontFileException(font, e.getMessage());
				}
				loadedFontCount++;
				
				//push the font into our map
				IntMap<FontData> sizeMap = dataMap.get(font.key);
				if (sizeMap==null) {
					sizeMap = new IntMap<FontData>();
					dataMap.put(font.key, sizeMap);
				}
				FontData fdata = new FontData();
				fdata.data = data;
				fdata.settings = s;
				fdata.item = font;
				
				sizeMap.put(sizes[i], fdata);
				
				if (listener!=null) {
					loadedFontCount++;
					listener.onFontLoad(loadedFontCount, totalFonts);
				}
			}
			gen.dispose();
		}
		
		FontPack pack = new FontPack();
		pack.atlas = packer;
		pack.dataMap = dataMap;
		
		//after we've packed all the glyphs... save our pixmaps
		Array<Page> pages = packer.getPages();
		
		System.out.println("---- Generated "+totalFonts+" bitmap font(s)");
		System.out.println("\t"+totalGlyphs+" glyphs packed into "+pages.size+" texture page(s)");
		System.out.println();
		
		if (outDir==null)
			return pack;
			
		//write the PNGs and get the returned references
		String[] refs = BitmapFontWriter.writePixmaps(pages, outDir, imageOutName);
		
		System.out.println("---- Output PNG files");
		for (String s : refs)
			System.out.println("\t"+s);
		
		//scaleW / scaleH -- size of first pixmap
		int w = pages.get(0).getPixmap().getWidth();
		int h = pages.get(0).getPixmap().getHeight();
		
		System.out.println();
		System.out.println("---- Output FNT files");
		//write each bitmap font
		for (IntMap<FontData> sizes : dataMap.values()) {
			if (sizes==null)
				continue;
			for (IntMap.Entry<FontData> e : sizes.entries()) {
				int size = e.key;
				FontData fdata = e.value;
				BitmapFontData data = fdata.data;
				Settings s = fdata.settings;
				FontItem item = fdata.item;
				
				//setup the font info header...
				String faceName = item.name!=null&&item.name.length()!=0 ? item.name : item.key; 
				FontInfo info = new FontInfo(faceName, size);
				info.padding = new BitmapFontWriter.Padding(s.paddingTop, s.paddingBottom, s.paddingLeft, s.paddingRight);
				
				//write the font
				String fntFile = item.key+"-"+size+".fnt";
				BitmapFontWriter.writeFont(data, refs, outDir.child(fntFile), info, w, h);
				
				System.out.println("\t"+fntFile);
				
			}
		}
		
		return pack;
	}
	
	static void tickProgress() {
		loadedGlyphs++;
		if (listener!=null) {
			listener.onGlyphLoad(loadedGlyphs, totalGlyphs);
		}
	}
	
	public static interface ProgressListener {
		
		public void onGlyphLoad(int loaded, int total);
		public void onFontLoad(int loaded, int total);
	}
	
	static class ProgressFilter implements FreeTypeFilter {
		
		@Override
		public Pixmap apply (Pixmap pixmap) {
			tickProgress();
			return pixmap;
		}

		@Override
		public int left () {
			return 0;
		}

		@Override
		public int top () {
			return 0;
		}
		
	}
	
	public static class InvalidFontFileException extends IOException {
		
		private static final long serialVersionUID = 1L;
		
		public final FontItem item;

		public InvalidFontFileException(FontItem item, String msg) {
			super("Unable to load font at path "+item.path+": "+msg);
			this.item = item;
		}
		
		public InvalidFontFileException(FontItem item) {
			super("Unable to load font at path "+item.path);
			this.item = item;
		}
	}
}
