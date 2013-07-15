package mdesl.font;

import javax.swing.SwingUtilities;

import mdesl.font.FontPackGUI.BGStyle;
import mdesl.font.FontPackTool.FontData;
import mdesl.font.FontPackTool.FontPack;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker.Page;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

public class TestFontPanel implements ApplicationListener {
	
	Array<FontElement> fonts;
	TextureRegion[] regions;
	
	SpriteBatch batch;
	OrthographicCamera cam;
	BGStyle background;
	FontPackGUI gui;
	
	public TestFontPanel(BGStyle background, FontPackGUI gui) {
		this.background = background;
		this.gui = gui;
	}
	
	class FontElement  implements Comparable<FontElement> {
		BitmapFont font;
		String key;
		String name;
		int size;
		
		@Override
		public int compareTo (FontElement o) {
			if (key!=null && o.key!=null && !key.equals(o.key))
				return o.key.compareToIgnoreCase(key);
			return o.size - size;
		}
	}
	
	@Override
	public void create () {
		cam = new OrthographicCamera();
		batch = new SpriteBatch();
		
	}
	
	@Override
	public void dispose () {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pause () {
		// TODO Auto-generated method stub
		
	}
	
	public void update(final FontPack pack, final boolean flip, BGStyle background) {
		this.background = background;
		Gdx.app.postRunnable(new Runnable() {
			public void run() {
				//dispose old fonts...
				if (regions!=null) {
					for (TextureRegion r : regions) {
						r.getTexture().dispose();
					}
				}
				
				//first get textures from the atlas
				Array<Page> pages = pack.atlas.getPages();
				regions = new TextureRegion[pages.size];
				
				for (int i=0; i<pages.size; i++) {
					Pixmap pix = pages.get(i).getPixmap();
					Texture tex = new Texture(pix);
					tex.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
					regions[i] = new TextureRegion(tex);
					
					//NOTE: don't dispose pixmaps since we will dispose them in the GUI
				}
				
				fonts = new Array<FontElement>();
				
				//generate BitmapFonts from data
				for (IntMap<FontData> sizeMap : pack.dataMap.values()) {
					if (sizeMap==null)
						continue;
					for (IntMap.Entry<FontData> e : sizeMap.entries()) {
						int size = e.key;
						FontData data = e.value;
						
						FontElement element = new FontElement();
						element.size = size;
						element.key = data.item.key;
						element.name = data.item.name != null && data.item.name.length()!=0 ? data.item.name : data.item.key;
						element.font = new BitmapFont(data.data, regions, flip);
						
						fonts.add(element);
					}
				}
				
				fonts.sort();
			}
		});
	}
	

	@Override
	public void render () {
		Gdx.gl.glClearColor(
					background.rgb.getRed() / 255f,
					background.rgb.getGreen() / 255f,
					background.rgb.getBlue() / 255f,
					background.rgb.getAlpha() / 255f);
		
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		
		final String text = "The quick brown fox jumps over the lazy dog";
		if (fonts!=null) {
			batch.begin();
			
			int x = 5;
			int y = 0;
			
			for (FontElement e : fonts) {
				y += e.font.getLineHeight() + 5;
				
				e.font.draw(batch, e.name + " " + e.size+": " +text, x, y);
			}
			
			batch.end();
		}
		
	}

	@Override
	public void resume () {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resize (int w, int h) {
		cam.setToOrtho(false,  w,  h);
		batch.setProjectionMatrix(cam.combined);
	}
}
