package mdesl.font;

import java.io.IOException;

import mdesl.font.BitmapFontWriter.OutputFormat;
import mdesl.font.FontPackTool.FontItem;
import mdesl.font.FontPackTool.FontPack;
import mdesl.font.FontPackTool.FontPackDocument;
import mdesl.font.FontPackTool.Settings;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.SharedLibraryLoader;

public class FontPackToolTest {
	
	public static void main(String[] args) throws IOException {
		//necessary to use Pixmap and friends
		new SharedLibraryLoader("libs/gdx-natives.jar").load("gdx");
		new SharedLibraryLoader("libs/gdx-freetype-natives.jar").load("gdx-freetype");
		
		//create a test document for the settings
		FontPackDocument doc = createDocument();
		
		//the output directory for our bitmap fonts and images
		FileHandle outDir = new FileHandle("out");
		
		//the file name of the image(s) that will be generated from the glyphs
		String imageOutName = "fonts";
		
		//Optional: chnage format, i.e. for a different rendering engine (like Pixi.js)
		//BitmapFontWriter.setOutputFormat(OutputFormat.XML);
		
		//right now the tool returns a PixmapPacker; this is likely to change in future releases...
		FontPack pack = FontPackTool.pack(doc, outDir, imageOutName);
		pack.atlas.dispose();
	}
	
	public static FontPackDocument createDocument() {
		FontPackDocument doc = new FontPackDocument();
		
		//resulting atlas width and height.. adjust to taste
		doc.atlasHeight = 512;
		doc.atlasWidth = 512;
		
		//spacing used for PixmapPacker
		doc.spacing = 1;
		
		//the size(s) of fonts we want to pack into the atlas
		doc.defaultSettings.sizes = new int[] { 12, 16, 18, 24, 32 };
		
		//Add each font... 
		//You will need to adjust this to include the necessary characters + fonts for each supported language
		doc.fonts.add( font("English", "Arial", "Arial.ttf", FontPackTool.ABRIDGED_CHARS) );
		doc.fonts.add( font("Korean", "UnBom", "unbom.ttf", "한국어/조선�?") );
		doc.fonts.add( font("Thai", "Garuda", "garuda.ttf", "วรณยุ�?ต์") );
		
		return doc;
	}
	
	private static FontItem font(String key, String fontFace, String fontFile, String characters) {
		//the "key" that will make up the resulting file name, and the TTF file to read from
		FontItem item = new FontItem(key, fontFile);
		
		//if the settings for FontItem is null, then the defaultSettings will be used from our doc
		
		//Here we will use individual settings to define the character set for each language
		item.settings = new Settings();
		item.settings.characters = characters;
		
		/////////// Drop shadow
		//enable the shadow filter
		item.settings.glow = true;
		
		//shadow offset
		item.settings.glowOffsetX = 1;
		item.settings.glowOffsetY = 1;
		
		//blur strength.. leave either at zero for no blur
		item.settings.glowBlurRadius = 1;
		item.settings.glowBlurIterations = 1;
		
		//the shadow color, we'll use semi-transparent black
		item.settings.glowColor = new Color(0f, 0f, 0f, 0.75f);

		/////////// Padding
		//add a bit of right and bottom padding to account for blurred shadow
		item.settings.paddingTop = 0;
		item.settings.paddingLeft = 0;
		item.settings.paddingRight = 3;
		item.settings.paddingBottom = 3;
		
		//FontItem "name" parameter is optional; this can be used to embed the font face (e.g. "Helvetica Neue")
		//into the BMFont file. If it's null or an empty string, then the key will be used in the file instead
		item.name = fontFace;
			
		//The resulting BMFont file will have the filename format "key-N.fnt" where N is the size of that font 
		return item;
	}
}
