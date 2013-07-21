package mdesl.font;

import mdesl.font.FontPackGUI.BGStyle;
import mdesl.font.FontPackTool.FontData;
import mdesl.font.FontPackTool.FontPack;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.PixmapPacker.Page;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

public class TestFontPanel implements ApplicationListener {
	
	Array<FontElement> fonts;
	TextureRegion[] regions;
	
	SpriteBatch batch;
	OrthographicCamera cam;
	BGStyle background;
	FontPackGUI gui;
	
	Stage stage;
	TextField input;
	Label labelInput, labelScale, scaleAmt;
	Slider scaleSlider;
	
	ToggleBox linearFiltering;
	
	Skin skin;
	
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
		
//		Gdx.gl.glClearColor(
//			background.rgb.getRed() / 255f,
//			background.rgb.getGreen() / 255f,
//			background.rgb.getBlue() / 255f,
//			background.rgb.getAlpha() / 255f);
//		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		
//		Gdx.graphics.setContinuousRendering(false);
//		Gdx.graphics.requestRendering();
		
		cam = new OrthographicCamera();
		batch = new SpriteBatch();
		
		stage = new Stage();
		skin = new Skin(Gdx.files.internal("data/uiskin.json"));
		input = new TextField("", skin);
		
		//can't use Table here since it will conflict with the Swing Table toolkit
		//this is why static is shit -.-
		labelInput = new Label("Sample Text:", skin);
		labelScale = new Label("Scale:", skin);
		scaleAmt = new Label("1.0", skin);
		
		labelInput.setHeight(input.getHeight());
		labelInput.setPosition(10, Gdx.graphics.getHeight() - labelInput.getHeight() - 5);
		input.setPosition(labelInput.getX()+labelInput.getWidth()+10, Gdx.graphics.getHeight() - input.getHeight() - 5);
		
		scaleSlider = new Slider(0, 3, 0.05f, false, skin);
		scaleSlider.setSnapToValues(new float[] { 0.0f, 0.5f, 1.0f}, 0.05f);
		
		scaleSlider.addListener(new ChangeListener() {
			@Override
			public void changed (ChangeEvent arg0, Actor arg1) {
				scaleAmt.setText(String.format("%.2f", scaleSlider.getValue()));
			}
		});
		scaleSlider.setValue(1.0f);
		scaleAmt.setText(String.format("%.2f", scaleSlider.getValue()));
		
		linearFiltering = new ToggleBox("Linear Filtering", skin);
		linearFiltering.addListener(new ClickListener() {
			public void clicked(InputEvent ev, float x, float y) {
				updateFiltering();
			}
		});
		
		scaleAmt.setHeight(scaleSlider.getHeight());
		labelScale.setHeight(scaleSlider.getHeight());
		
		labelScale.setPosition(input.getX() - 10 - labelScale.getWidth(), labelInput.getY() - labelInput.getHeight() - 5);
		scaleSlider.setPosition(input.getX(), input.getY() - input.getHeight() - 5);
		scaleAmt.setPosition(scaleSlider.getX() + scaleSlider.getWidth() + 5, scaleSlider.getY());
		
		linearFiltering.setPosition(input.getX(), scaleSlider.getY() - scaleSlider.getHeight() - 10);
		
		Gdx.input.setInputProcessor(stage);
		stage.addActor(labelInput);
		stage.addActor(input);
		stage.addActor(labelScale);
		stage.addActor(scaleSlider);
		stage.addActor(scaleAmt);
		stage.addActor(linearFiltering);
	
		
	}
	
	//our own little hack since Table doesn't work with Swing alongside :\
	class ToggleBox extends Widget {
		
		Skin skin;
		BitmapFont font;
		Drawable active, checked;
		String text;
		private boolean selected = false;
		final int pad = 5;
		
		public ToggleBox(String text, Skin skin) {
			this.text = text;
			this.skin = skin;
			font = skin.getFont("default-font");
			checked = skin.getDrawable("check-on");
			active = skin.getDrawable("check-off");
			setTouchable(Touchable.enabled);
			addListener(new ClickListener() {
				public void clicked(InputEvent ev, float x, float y) {
					setSelected(!isSelected());
				}
			});
			TextBounds fb = font.getBounds(text);
			setSize(checked.getMinWidth() + pad + fb.width, Math.max(checked.getMinHeight(), font.getCapHeight()));
		}
		
		public  void setSelected(boolean b) {
			boolean old = this.selected;
			this.selected = b;
			if (old!=b) {
				frameDirty();
			}
		}
		
		public  boolean isSelected() {
			return selected;
		}
		
		public void draw(SpriteBatch batch, float parentAlpha) {
			super.draw(batch, parentAlpha);
			
			(selected ? checked : active).draw(batch, getX(), getY(), checked.getMinWidth(), checked.getMinHeight());
			font.draw(batch, text, getX() + checked.getMinWidth() + pad, getY() + font.getCapHeight());
		}
	}
	
	private void frameDirty() {
//		Gdx.app.postRunnable(new Runnable() {
//			
//			@Override
//			public void run () {
//				Gdx.graphics.requestRendering();
//			}
//		});
	}
	
	@Override
	public void dispose () {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pause () {
		// TODO Auto-generated method stub
		
	}
	
	public void updateFiltering() {
		if (regions == null)
			return;
		TextureFilter f = linearFiltering.isSelected() ? TextureFilter.Linear : TextureFilter.Nearest;
		for (TextureRegion r : regions)
			r.getTexture().setFilter(f, f);
	}
	
	public void update(final FontPack pack, final boolean flip, BGStyle background) {
		this.background = background;
		
		if (stage!=null)
			stage.setKeyboardFocus(null);
		
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
				
				TextureFilter filter = linearFiltering.selected ? TextureFilter.Linear : TextureFilter.Nearest;
				
				for (int i=0; i<pages.size; i++) {
					Pixmap pix = pages.get(i).getPixmap();
					Texture tex = new Texture(pix);
					tex.setFilter(filter, filter);
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
		
		boolean hasUserInput = false;
		if (input.getText()!=null && input.getText().length()!=0)
			hasUserInput = true;
		final String text =  hasUserInput
					? input.getText()
					: "The quick brown fox jumps over the lazy dog";
		
		float scale = scaleSlider.getValue();
		
		cam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		batch.setProjectionMatrix(cam.combined);
		
		batch.getProjectionMatrix().scale(scale, scale, 0.0f);
		
//		batch.getTransformMatrix().scale(scale, scale, 0.0f);
		
		if (fonts!=null) {
			batch.begin();
			
			int x = 5;
			int y = 0;
			
			for (FontElement e : fonts) {
				y += e.font.getLineHeight() + 5;
				String str = hasUserInput ? text : e.name + " " + e.size+": " +text;
				e.font.draw(batch, str, x, y);
			}
			
			batch.end();
		}
		
		input.setY(Gdx.graphics.getHeight() - input.getHeight() - 5);
		labelInput.setY(Gdx.graphics.getHeight() - input.getHeight() - 5);
		
		labelScale.setY(labelInput.getY() - labelInput.getHeight() - 5);
		scaleSlider.setY(input.getY() - input.getHeight() - 5);
		scaleAmt.setY(scaleSlider.getY());
		

		stage.act();
		stage.draw();
	}

	@Override
	public void resume () {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resize (int w, int h) {
		cam.setToOrtho(false,  w,  h);
		batch.setProjectionMatrix(cam.combined);
		stage.setViewport(w, h, false);
	}
}
