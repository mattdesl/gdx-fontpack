package mdesl.font;

import java.io.IOException;

import mdesl.font.FontPackTool.FontItem;
import mdesl.font.FontPackTool.FontPackDocument;
import mdesl.font.FontPackTool.Settings;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
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
		
		//right now the tool returns a PixmapPacker; this is likely to change in future releases...
		PixmapPacker packer = FontPackTool.pack(doc, outDir, imageOutName);
		packer.dispose();
	}
	
	public static FontPackDocument createDocument() {
		FontPackDocument doc = new FontPackDocument();
		
		//resulting atlas width and height.. adjust to taste
		doc.atlasHeight = 512;
		doc.atlasWidth = 512;
		
		//spacing used for PixmapPacker
		doc.spacing = 1;
		
		//default character set
		doc.defaultSettings.characters = FontPackTool.ABRIDGED_CHARS;
		
		//enable drop shadow, i.e. outer glow
		doc.defaultSettings.glow = true;
		
		//shadow offset
		doc.defaultSettings.glowOffsetX = 1;
		doc.defaultSettings.glowOffsetY = 1;
		
		//blur strength.. leave either at zero for no blur
		doc.defaultSettings.glowBlurRadius = 1;
		doc.defaultSettings.glowBlurIterations = 1;
		
		//add a bit of right and bottom padding to account for blurred shadow
		doc.defaultSettings.paddingTop = 0;
		doc.defaultSettings.paddingLeft = 0;
		doc.defaultSettings.paddingRight = 2;
		doc.defaultSettings.paddingBottom = 2;
		
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
		//each FontItem can optionally have its own settings
		FontItem item = new FontItem(key, fontFile);
		item.settings = new Settings();
		item.settings.characters = characters;
		
		//FontItem "name" parameter is optional; this can be used to embed the font face (e.g. "Helvetica Neue")
		//into the BMFont file. If it's null or an empty string, then the key will be used in the file instead
		item.name = fontFace;
		
		//The resulting BMFont file will have the filename format "key-N.fnt" where N is the size of that font 
		return item;
	}
}
