
package mdesl.font;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import mdesl.font.BitmapFontWriter.OutputFormat;
import mdesl.font.FileUtil.BrowseType;
import mdesl.font.FileUtil.PrefType;
import mdesl.font.FileUtil.FileType;
import mdesl.font.FontPackTool.FontItem;
import mdesl.font.FontPackTool.FontPack;
import mdesl.font.FontPackTool.FontPackDocument;
import mdesl.font.FontPackTool.InvalidFontFileException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglCanvas;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.PixmapPacker.Page;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import com.esotericsoftware.tablelayout.swing.Table;

public class FontPackGUI extends JFrame implements FontPackTool.ProgressListener {
	
	private static boolean useJFileChooser = true;
	
	private static final boolean DISABLE_GL_ON_HIDE = true;
	
	public static void main(String[] args) {
		new SharedLibraryLoader("libs/gdx-natives.jar").load("gdx");
		new SharedLibraryLoader("libs/gdx-freetype-natives.jar").load("gdx-freetype");
		
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("osx") || osName.contains("mac")) {
			useJFileChooser = false;
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run () {
				new FontPackGUI();
			}
		});
	}
	
	final int MAX_SPINNER_VALUE = 999;
	
	JLabel statusBar;
	ImageIcon iconWarn, iconErr, iconInfo;
	JList fontList;
	JTextField outPathField;
	JSpinner sizeWidth, sizeHeight;
	JSpinner spacingSpinner;
	JTextField sizeField;
	JTextField outNameField;
	
	IconButton outPathButton;
	IconButton addFntBtn, delFntBtn, editFntBtn;
	
	DefaultListModel listModel = new DefaultListModel();
	
	JCheckBox shadowCheckbox;
	JButton shadowButton;
	
	Preferences prefs = Preferences.userNodeForPackage(FontPackGUI.class);
	FileUtil fileChooser;
	
	Table listTable = new Table();
	Table settingsTable = new Table();
	Table outputTable = new Table();
	
	AtlasCache atlasCache;
	AtlasPanel atlasPanel = new AtlasPanel();
	
	FontEditDialog fntDiag;
	
	JComboBox outFormatBox;
	
	JCheckBox testCheckBox;
	JLabel pageLabel;
	IconButton pageLeft, pageRight;
	Table root = new Table();
	JPanel statusPanel;
	BGStyle background = BGStyle.Gray;
	
	FontPack fontPack;
	
	ShadowEditDialog shadowEdit;
	EditCharsDialog editChars;

	FontItem lastInvalidFont;
	
	CardLayout outputCardLayout; 
	JPanel outputCards;
	TestPanel testPanel;
	Table glyphTable = new Table();
	Timer glDisposeTimer = new Timer(1000, new ActionListener() {

		@Override
		public void actionPerformed (ActionEvent arg0) {
			System.out.println("dsipose");
		}
		
	});
	
	String charSet = FontPackTool.ABRIDGED_CHARS;
	
	public static final String GLYPHS_VIEW = "GLYPHS_VIEW";
	public static final String TEST_VIEW = "TEST_VIEW";
	
	enum BGStyle {
		Gray(Color.GRAY),
		Black(Color.BLACK),
		White(Color.WHITE),
		CheckerLight(Color.GRAY),
		CheckerDark(Color.GRAY);
		
		
		public final Color rgb;
		
		BGStyle(Color rgb) {
			this.rgb = rgb;
		}
	}
	
	public FontPackGUI() {
		super("Font Packer");
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception e) { System.out.println("Error setting Java LAF: " + e); }

		fileChooser = new FileUtil(this, useJFileChooser, prefs);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setupGUI();
		
		addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing (WindowEvent arg0) {
				if (testPanel!=null)
					testPanel.canvas.stop();
			}
			
			@Override
			public void windowClosed (WindowEvent arg0) {
				System.exit(0);
			}
		});
		
		updateOutput();
		
		setSize(950, 650);
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	
	
	private void setupGUI() {
//		root.debug();
		
		setupListTable();
		setupSettingsTable();
		
		Table left = new Table();
		
		left.addCell(listTable).expand().fill();
		left.row();
		
		
		left.addCell(settingsTable).expandX().fillX().row();
		
		//GENERATE
		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed (ActionEvent arg0) {
				updateOutput(true);
			}
		});
		
		left.addCell(saveButton).padTop(5);
		
		//left.addCell(new JSeparator()).expandX().fillX().padTop(5).padBottom(5).row();
		
		

		
//		JLabel pathInfoLabel = new JLabel("Images and fnt files will be saved to the directory");
//		pathInfoLabel.setFont(pathInfoLabel.getFont().deriveFont(9f));
//		outTable.addCell(pathInfoLabel).right();
		
		
//		settingsTable.addCell(outTable).expandX().fillX();
//		settingsTable.row();
		
		//output
		atlasPanel = new AtlasPanel();
//		Dimension d = new Dimension(512, 512);
//		atlasPanel.setPreferredSize(d);
//		atlasPanel.setMinimumSize(d);
		
		Table styleTable = new Table();
		final JComboBox bgStyle = new JComboBox(BGStyle.values());
		bgStyle.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed (ActionEvent arg0) {
				background = (BGStyle)bgStyle.getSelectedItem();
				atlasPanel.repaint();
			}
		});
		styleTable.left().padTop(5).padBottom(5);
		styleTable.addCell("Background:").left();
		styleTable.addCell(bgStyle).left();
		
		styleTable.addCell("").expandX().fillX();
		
		pageLeft = new IconButton(this, "/data/arrow-left.png");
		pageRight = new IconButton(this, "/data/arrow-right.png");
		
		pageLeft.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed (ActionEvent arg0) {
				if (atlasCache==null)
					return;
				
				int ind = Math.max(0, atlasCache.curPage - 1);
				if (ind != atlasCache.curPage) {
					atlasCache.setPage(ind);
					updateAtlas();
				}
			}
		});
		pageRight.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed (ActionEvent arg0) {
				if (atlasCache==null)
					return;
				
				int ind = Math.min(atlasCache.size()-1, atlasCache.curPage + 1);
				if (ind != atlasCache.curPage) {
					atlasCache.setPage(ind);
					updateAtlas();
				}
			}
		});
		
		pageLabel = new JLabel("No Pages", JLabel.CENTER);
		styleTable.addCell(pageLeft);
		styleTable.addCell(pageLabel).padLeft(5).padRight(5).center();
		styleTable.addCell(pageRight);
		
		outputCardLayout = new CardLayout();
		outputCards = new JPanel(outputCardLayout);
		
		glyphTable.addCell(styleTable).expandX().fillX().row();
		glyphTable.addCell(atlasPanel).expand().fill();
		
		outputCards.add(glyphTable, GLYPHS_VIEW);
		
		outputTable.addCell(outputCards).expand().fill();
		
		root.addCell(left).fill().expand().left().pad(10).padRight(10);
		root.addCell(outputTable).prefSize(256, 512).expand().fill().pad(5);
		root.row();
		
		iconWarn = new ImageIcon(FontPackGUI.class.getResource("/data/icon_alert.gif"));
		iconErr = new ImageIcon(FontPackGUI.class.getResource("/data/icon_error.gif"));
		iconInfo = new ImageIcon(FontPackGUI.class.getResource("/data/icon_info.gif"));
		
		
		statusBar = new JLabel("Status");
		statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
		statusPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xd2d2d2)));
		statusPanel.setBackground(Color.WHITE);
		statusPanel.setOpaque(true);
		statusPanel.add(statusBar);
		root.addCell(statusPanel).center().expandX().fillX().colspan(2).height(statusPanel.getPreferredSize().height);
		
		status(null, "");
		
//		root.pad(10);
		root.top().left();
		
		setContentPane(root);
	}
	
	private void status(ImageIcon icon, String text) {
		statusPanel.setVisible( text!=null && text.length()!=0 );
		
		statusBar.setIcon(icon);
		statusBar.setText(text);
	}
	
	private Table hr(String text) {
		JLabel lbl = new JLabel(text);
		lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 10f));
		
		Table table = new Table();
		table.addCell(new JSeparator()).expandX().fillX();
		table.addCell(lbl).padLeft(5).padRight(5);
		table.addCell(new JSeparator()).expandX().fillX();
		return table;
	}
	
	private void setupSettingsTable() {
		settingsTable.top().left();
		
		final int HPAD = 5; 
		
		settingsTable.addCell(hr("settings")).expandX().fillX().padTop(20).padBottom(5).colspan(2).row();
		
		final IconButton editGlyphsBtn = new IconButton(this, "Edit", "/data/pencil.png");
		editGlyphsBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed (ActionEvent arg0) {
				if (editChars==null)
					editChars = new EditCharsDialog(FontPackGUI.this);
				
				String res = editChars.showEdit(charSet);
				if (res!=null) {
					if (res.length()==0) {
						JOptionPane.showMessageDialog(FontPackGUI.this, "No characters specified!", "Error", JOptionPane.ERROR_MESSAGE);
					} else {
						charSet = res;
						updateOutput();
					}
				}
			}
		});
		
		settingsTable.addCell("Glyphs:").right().padRight(HPAD);
		
		settingsTable.addCell(editGlyphsBtn).left();
		settingsTable.row();
		
		//WIDTH, HEIGHT
		settingsTable.addCell("Size:").right().padRight(HPAD);
		
		sizeWidth = new JSpinner(new SpinnerNumberModel(512, 1, 16384, 1));
		sizeHeight = new JSpinner(new SpinnerNumberModel(512, 1, 16384, 1));
		sizeWidth.setEditor(new JSpinner.NumberEditor(sizeWidth, "#"));
		sizeHeight.setEditor(new JSpinner.NumberEditor(sizeHeight, "#"));
		sizeWidth.addChangeListener(new UpdateChange());
		sizeHeight.addChangeListener(new UpdateChange());
		
		Table sizeTable = new Table();
		sizeTable.left();
		sizeTable.addCell(sizeWidth).left();
		sizeTable.addCell("x").padLeft(3).padRight(3).center();
		sizeTable.addCell(sizeHeight).left();
		settingsTable.addCell(sizeTable).expandX().fillX().row();
		
		//SPACING
		spacingSpinner = new JSpinner(new SpinnerNumberModel(1, 0, MAX_SPINNER_VALUE, 1));
		spacingSpinner.addChangeListener(new UpdateChange());
		
		settingsTable.addCell("Spacing:").right().padRight(HPAD);
		settingsTable.addCell(spacingSpinner).left();
		settingsTable.row();
		
		//PADDING
		settingsTable.addCell("Padding:").right().padRight(HPAD);
		
		Table padTable = paddingTable();
		settingsTable.addCell(padTable).left();
		
		//SHADOW
		shadowCheckbox = new JCheckBox("Enabled");
		
		shadowCheckbox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed (ActionEvent arg0) {
				shadowButton.setEnabled(shadowCheckbox.isSelected());
				updateOutput();
			}
		});
		shadowButton = new IconButton(this, "/data/pencil.png");
		shadowButton.setEnabled(false);
		shadowButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed (ActionEvent e) {
				if (shadowEdit==null) {
					shadowEdit = new ShadowEditDialog(FontPackGUI.this);
				}
				shadowEdit.showEdit();
			}
			
		});
		
		Table shadowTable = new Table();
		
		settingsTable.row();
		settingsTable.addCell("Shadow:").right().padRight(HPAD);
		shadowTable.addCell(shadowCheckbox);
		shadowTable.addCell(shadowButton);
		settingsTable.addCell(shadowTable).left();
		
		
		//SIZES
		sizeField = new JTextField("32, 18, 12");
		sizeField.addActionListener(new UpdateAction());
		((AbstractDocument)sizeField.getDocument()).setDocumentFilter(new SizeFieldFilter());
		
		settingsTable.row();
		settingsTable.addCell("Sizes:").right().padRight(HPAD);
		settingsTable.addCell(sizeField).expandX().fillX();
		settingsTable.row();
		
		JLabel infoLabel = new JLabel("Separate font sizes by commas and/or spaces");
		infoLabel.setFont(infoLabel.getFont().deriveFont(9f));
		settingsTable.addCell(infoLabel).colspan(2).right();
		
		
		//OUTPUT PATH
		Table outTable = new Table();
		
		outPathField = new JTextField();
		outPathButton = new IconButton(this, "/data/folder-2.png");
		
		outPathButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed (ActionEvent arg0) {
				File f = fileChooser.browse(BrowseType.Open, FileType.None, PrefType.FontSave, true, outPathField.getText());
				if (f!=null)
					outPathField.setText(f.getAbsolutePath());
			}
		});
		
		outNameField = new JTextField();
		
		TextPrompt prompt = new TextPrompt("fonts", outNameField);
		prompt.changeAlpha(0.25f);
		prompt.changeStyle(Font.ITALIC);
		
		settingsTable.row();
		
		settingsTable.addCell(hr("output")).expandX().fillX().padTop(20).padBottom(5).colspan(2).row();
		
		outFormatBox = new JComboBox(BitmapFontWriter.OutputFormat.values());
		
		settingsTable.addCell("Format:").right().padRight(HPAD);
		settingsTable.addCell(outFormatBox).left();
		settingsTable.row();
		
		settingsTable.addCell("Name:").right().padRight(HPAD);
		settingsTable.addCell(outNameField).expandX().fillX();
		settingsTable.row();
		
		settingsTable.addCell("Folder:").right().padRight(HPAD);
		outTable.addCell(outPathField).expandX().fillX();
		outTable.addCell(outPathButton);
		settingsTable.addCell(outTable).left().expandX().fillX();
		
		
//		left.addCell(hr("output")).expandX().fillX().padTop(20).padBottom(5).row();
//		left.addCell(outTable).expandX().fillX().row();
	}
	
	JSpinner padTop, padLeft, padBottom, padRight;
	
	private Table paddingTable() {
		Table spinnerTable = new Table();
		
		int max = MAX_SPINNER_VALUE;
		padTop = new JSpinner(new SpinnerNumberModel(0, -max, max, 1));
		padLeft = new JSpinner(new SpinnerNumberModel(0, -max, max, 1));
		padBottom = new JSpinner(new SpinnerNumberModel(0, -max, max, 1));
		padRight = new JSpinner(new SpinnerNumberModel(0, -max, max, 1));
		
		padTop.addChangeListener(new UpdateChange());
		padLeft.addChangeListener(new UpdateChange());
		padBottom.addChangeListener(new UpdateChange());
		padRight.addChangeListener(new UpdateChange());
		
//		spinnerTable.center();
		
		spinnerTable.addCell(padTop).center().colspan(2);
		spinnerTable.row();
		spinnerTable.addCell(padLeft).left();
		spinnerTable.addCell(padRight).right();
		spinnerTable.row();
		spinnerTable.addCell(padBottom).center().colspan(2);
		
		return spinnerTable;
	}
	
	private void setupListTable() {
		fontList = new JList();
		fontList.setModel(listModel);
		fontList.setCellRenderer(new FontCellRenderer());
		

		new FileDrop(fontList, new FileDrop.Listener() {
			public void filesDropped (java.io.File[] files) {
				for (int i = 0; i < files.length; i++) {
					FontItem item = new FontItem();
					String fName = files[i].getName();
					
					int idx = fName.lastIndexOf('.');
					if (idx>0)
						fName = fName.substring(0, idx);
					
					item.key = fName;
					item.path = files[i].getAbsolutePath();
					listModel.addElement(item);
				}
				updateOutput();
				
			}
		});		
		
		FontItem item = new FontItem("Arial", "Arial.ttf");
		listModel.addElement(item);
		
		addFntBtn = new IconButton(this, "/data/add.png");
		delFntBtn = new IconButton(this, "/data/delete.png");
		delFntBtn.setEnabled(false);
		editFntBtn = new IconButton(this, "/data/pencil.png");
		editFntBtn.setEnabled(false);
		
		addFntBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				final int index = fontList.getSelectedIndex();
				
				if (fntDiag==null)
					fntDiag = new FontEditDialog(FontPackGUI.this);
				
				FontItem retItem = fntDiag.showFont( null );
				if (retItem==null)
					return;
				
				listModel.add(index+1, retItem);
				fontList.setSelectedIndex(index+1);
				updateOutput();
			}
		});
		delFntBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int index = fontList.getSelectedIndex();
				if (index!=-1) {
					listModel.remove(index);
					if (listModel.size()>0) {
						fontList.setSelectedIndex(Math.max(0, index-1));
					}
					updateOutput();
				}
			}
		});

		editFntBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				FontItem item = (FontItem)fontList.getSelectedValue();
				
				if (item==null)
					return;
				
				if (fntDiag==null)
					fntDiag = new FontEditDialog(FontPackGUI.this);
				
				FontItem retItem = fntDiag.showFont( item );
				if (retItem==null)
					return;
				
				fontList.repaint();
				updateOutput();
			}
		});
		
		fontList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		fontList.addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int i = fontList.getSelectedIndex();
				delFntBtn.setEnabled(i!=-1);
				editFntBtn.setEnabled(i!=-1);
				if (i>=0) {
					
				}
			}
		});
		
		JScrollPane scroll = new JScrollPane(fontList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		
		listTable.top().left();
		
		
		listTable.addCell(hr("fonts")).expandX().fillX().padBottom(2).row();
		
		JLabel dragInfo = new JLabel("Drag font files below");
		dragInfo.setFont(dragInfo.getFont().deriveFont(9f));
		
		listTable.addCell(dragInfo).row().padTop(5);
		listTable.addCell(scroll).expand().fill();
		
		
		testCheckBox = new JCheckBox("Test Fonts");
		testCheckBox.setHorizontalTextPosition(JCheckBox.RIGHT);
		testCheckBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed (ActionEvent arg0) {
				
				boolean testMode = testCheckBox.isSelected();
				
				if (testMode) {
					if (testPanel==null) { 
						testPanel = new TestPanel();
						outputCards.add(testPanel, TEST_VIEW);
						outputCards.revalidate();	
					}
					
					testPanel.update();
					outputCardLayout.show(outputCards, testMode ? TEST_VIEW : GLYPHS_VIEW);
					
				} else {
					outputCardLayout.show(outputCards, testMode ? TEST_VIEW : GLYPHS_VIEW);
					onAtlasChanged(fontPack);
					
					if (testPanel!=null && DISABLE_GL_ON_HIDE) {
						testPanel.canvas.stop();
						outputCards.remove(testPanel);
						testPanel = null;
					}
				}
				
//				if (testPanel!=null)
//					testPanel.setRendering(testMode);
			}
		});
		
		Table btnTable = new Table();
		btnTable.left();
		
		btnTable.addCell(addFntBtn).size(28);
		btnTable.addCell(delFntBtn).size(28);
		btnTable.addCell(editFntBtn).size(28);
		btnTable.addCell("").expandX().fillX();
		btnTable.addCell(testCheckBox).right();
		
		listTable.row();
		listTable.addCell(btnTable).expandX().fillX();
	}
	
	private void glCanvasStopped() {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run () {
				//outputCards.remove(testPanel);
				testPanel = null;
			}
		});
	}
	
	class IconButton extends JButton {
		
		Color btnBG_Active = new Color(0.85f, 0.85f, 0.85f);
		Color btnBG_Pressed = new Color(0.75f, 0.75f, 0.75f);
		final int ICON_SIZE = 24;
		final int ICON_PAD = 3;
		public boolean entered = false;
		public boolean pressed = false;
		private Window parent;
		boolean hasText = false;
		
		public IconButton(Window parent, String resourcePath) {
			this(parent, null, resourcePath);
		}
		
		public IconButton(Window parent, String text, String resourcePath) {
			this.parent = parent;
			setText(text);
			setIcon(new ImageIcon(FontPackGUI.class.getResource(resourcePath)));
			setContentAreaFilled(false);
			setBorderPainted(false);
			
			int w = ICON_SIZE;
			setRequestFocusEnabled(false);
			if (text==null) {
				
			} else {
				hasText = true;
				setHorizontalTextPosition(LEFT);
				w = getPreferredSize().width;
			}
			
			Dimension d = new Dimension(w, ICON_SIZE);
			setSize(d);
			setPreferredSize(d);
			setMaximumSize(d);
			
			addMouseListener(new MouseAdapter() {

				@Override
				public void mouseEntered (MouseEvent arg0) {
					entered = true;
					repaint();
				}

				@Override
				public void mouseExited (MouseEvent arg0) {
					entered = false;
					repaint();
				}

				@Override
				public void mousePressed (MouseEvent arg0) {
					pressed = true;
					repaint();
				}

				@Override
				public void mouseReleased (MouseEvent arg0) {
					pressed = false;
					repaint();
				}
			});
		}
		
		protected void paintComponent(Graphics g) {
			if (!parent.isActive())
				entered = pressed = false;
			
			if (entered && isEnabled()) {
				g.setColor(pressed ? btnBG_Pressed : btnBG_Active);
				g.setClip(0, 0, getWidth(), getHeight());
				((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				
				
				int w = hasText ? getWidth() : ICON_SIZE;
				int x = hasText ? 0 : (getWidth()/2 - ICON_SIZE/2);
				
				g.fillRoundRect(x, getHeight()/2 - ICON_SIZE/2, w, ICON_SIZE, 10, 10);
			}
			if (pressed && isEnabled())
				g.translate(1, 1);
			super.paintComponent(g);
			if (pressed && isEnabled())
				g.translate(1,  1);
		}

		
	}
	
	
	FontPackDocument doc = new FontPackDocument();
	
	void updateListRenderer() {
		
	}
	
	void updateOutput() {
		updateOutput(false);
	}
	
	String nameWithoutExtension(File file) {
		String str = file.getName();
		int i = str.lastIndexOf('.');
		if (i==-1)
			return str;
		return str.substring(0, i);
	}
	
	void updateOutput(boolean save) {
		status(null, "");
//		try {
//			prefs.clear();
//		} catch (BackingStoreException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		
		lastInvalidFont = null;
		
		FileHandle outDir = null;
		String imageOutName = outNameField.getText().length()==0 ? "fonts" : outNameField.getText();
		if (save) {
			String fieldText = outPathField.getText();
			if (fieldText.length()==0) {
			
				File f = fileChooser.browse(BrowseType.Open, FileType.None, PrefType.FontSave, true, fieldText);
				if (f==null) //cancelled
					return;
				fieldText = f.getAbsolutePath();
				outPathField.setText(fieldText);
			}
			outDir = new FileHandle(fieldText);
		}
		
		doc.atlasWidth = ((Number)sizeWidth.getValue()).intValue();
		doc.atlasHeight = ((Number)sizeHeight.getValue()).intValue();
		doc.spacing = ((Number)spacingSpinner.getValue()).intValue();
		doc.defaultSettings.characters = charSet;
		
		doc.defaultSettings.glow = shadowCheckbox.isSelected();
		doc.defaultSettings.glowOffsetX = (shadowEdit!=null) ? ((Number)shadowEdit.offX.getValue()).intValue() : 1;
		doc.defaultSettings.glowOffsetY = (shadowEdit!=null) ? ((Number)shadowEdit.offY.getValue()).intValue()  : 1;
		doc.defaultSettings.glowBlurRadius = (shadowEdit!=null) ? ((Number)shadowEdit.spinRadius.getValue()).intValue()  : 1;
		doc.defaultSettings.glowBlurIterations = (shadowEdit!=null) ? ((Number)shadowEdit.spinIterations.getValue()).intValue()  : 1;
		
		double alpha = (shadowEdit!=null) ? ((Number)shadowEdit.sliderSpinner.getValue()).doubleValue() : 0.75;
		com.badlogic.gdx.graphics.Color c = new com.badlogic.gdx.graphics.Color(0, 0, 0, (float)alpha);
		doc.defaultSettings.glowColor = c;
		
		doc.defaultSettings.paddingTop = ((Number)padTop.getValue()).intValue();
		doc.defaultSettings.paddingLeft = ((Number)padLeft.getValue()).intValue();
		doc.defaultSettings.paddingRight = ((Number)padRight.getValue()).intValue();
		doc.defaultSettings.paddingBottom = ((Number)padBottom.getValue()).intValue();
		
		doc.defaultSettings.sizes = strToSizes(sizeField.getText());
		
		doc.fonts.clear();
		for (int i=0; i<listModel.size(); i++) {
			FontItem item = (FontItem)listModel.get(i);
			File validFile = fontItemFile(item);
			
			if (validFile==null)
				status(iconWarn, "One or more of the fonts were skipped due to invalid path/key");
			else
				doc.fonts.add( item );
		}
		
		try {
			BitmapFontWriter.setOutputFormat( (OutputFormat)outFormatBox.getSelectedItem() );
			fontPack = FontPackTool.pack(doc, outDir, imageOutName);
			onAtlasChanged(fontPack);
			
//			packer.dispose();
		}
		catch (InvalidFontFileException e) {
			status(iconErr, e.getMessage());
			lastInvalidFont = e.item;
		} catch (Exception e) {
			status(iconErr, e.getMessage());
		}
	}
	
	class AtlasCache {
		Pixmap[] pixmaps;
		BufferedImage[] images;
		int w, h, curPage;
		Array<Page> oldPages;
		
		public AtlasCache(Array<Page> pages) {
			pixmaps = new Pixmap[pages.size];
			setup(pages);
		}
		
		public void setup(Array<Page> pages) {
			if (oldPages != pages) {
				//dispose old pixmaps in memory
				for (int i=0; i<pixmaps.length; i++) {
					if (pixmaps[i]!=null) {
						pixmaps[i].dispose();
						pixmaps[i] = null;
					}
				}
			}
			
			curPage = 0;
			
			//new pixmaps array
			pixmaps = new Pixmap[pages.size];
			images = new BufferedImage[pages.size];
			
			for (int i=0; i<pixmaps.length; i++) {
				pixmaps[i] = pages.get(i).getPixmap();
			}
			
			//System.out.println("size "+pages.size);
			
			this.oldPages = pages;
			
			if (pages.size==0) {
				return;
			}
			
			w = pixmaps[0].getWidth();
			h = pixmaps[0].getHeight();
		}
		
		public void setPage(int index) {
			curPage = index;
			if (size() == 0)
				return;
			
			if (images[index]==null) {
				images[index] = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
				int[] argb = new int[w * h];
				
				//copy pixmap to AWT
				toARGB(pixmaps[index], argb);
				images[index].setRGB(0,  0,  w, h, argb, 0, w);
			}
		}
		
		public BufferedImage page() {
			return size()!=0 ? images[curPage] : null;
		}
		
		public int size() {
			return pixmaps.length;
		}
	}
	
	//on set page
	private void updateAtlas() {
		int index = atlasCache!=null ? atlasCache.curPage : 0;
		
		pageLabel.setText(listModel.size()==0 ? "No Pages" : "Page: "+(index+1)+" / "+atlasCache.size());
		outputTable.revalidate();
		outputTable.repaint();
		pageLeft.setEnabled( index>0 );
		pageRight.setEnabled( index<atlasCache.size()-1 );
	}
	
	//on new generation
	private void onAtlasChanged(FontPack pack) {
		if (testPanel!=null && testPanel.isShowing()) {
//			System.out.println("showing test");
			
			testPanel.update();
			if (atlasCache!=null) {
				atlasCache.setPage(0);
			}
			return;
		}
		
		Array<Page> pages = pack.atlas.getPages();
		if (atlasCache==null)
			atlasCache = new AtlasCache(pages);
		else
			atlasCache.setup(pages);
		
		int index = atlasCache.curPage; //last index
		
		//reset to page zero
		if (index > atlasCache.size()-1)
			index = 0;
		
		//set page
		atlasCache.setPage(index);
		
//		Dimension d = new Dimension(512, 512);
//		atlasPanel.setPreferredSize(d);
//		atlasPanel.setMinimumSize(d);
		
		updateAtlas();
	}
	
	public void toARGB(Pixmap pix, int[] argb) {
		int w = pix.getWidth();
		int h = pix.getHeight();
		
		if (pix.getFormat()==Format.RGBA8888) {
			ByteBuffer buf = pix.getPixels();
			buf.rewind();
			for (int i=0; i<w*h; i++) {
				int R = buf.get() & 0xFF;
				int G = buf.get() & 0xFF;
				int B = buf.get() & 0xFF;
				int A = buf.get() & 0xFF;
				
				argb[i] = (A << 24) | (R << 16) | (G << 8) | B;
			}
			buf.flip();
		} else {
			throw new UnsupportedOperationException("only RGBA8888 has been implemented");
		}
	}
	
	
	@Override
	public void onGlyphLoad (int loaded, int total) {
		System.out.println("Load glyph "+loaded+" / "+total);
	}

	@Override
	public void onFontLoad (int loaded, int total) {
		System.out.println("Load font "+loaded+" / "+total);
	}

	String sizesToStr(int[] sizes) {
		if (sizes==null)
			return "";
		StringBuffer buf = new StringBuffer();
		for (int s : sizes)
			buf.append(s).append(" ");
		return buf.toString().trim();
	}

	static int[] strToSizes(String str) {
		String[] spl = str.split("[ \t,]+");
		IntArray ar = new IntArray();
		for (String s : spl) {
			try {
				int i = Integer.parseInt(s);
				if (i>1 && !ar.contains(i))
					ar.add(i);
			} catch (NumberFormatException e) {}
		}
		return ar.toArray();
	}
	
	class SizeFieldFilter extends DocumentFilter {
		
		public void insertString(FilterBypass fb, int offs, String str,
				AttributeSet a) throws BadLocationException {
			if (check(str))
				super.insertString(fb, offs, str, a);
		}
		
		public void replace(FilterBypass fb, int offs, int length, String str,
				AttributeSet a) throws BadLocationException {
			if (check(str))
				super.replace(fb, offs, length, str, a);
		}
		
		boolean check(String str) {
			for (int i=0; i<str.length(); i++) {
				char c = str.charAt(i);
				if (!Character.isDigit(c) && c!=' ' && c!='\t' && c!=',')
					return false;
			}
			return true;
		}
		
		public void remove(FilterBypass fb, int offs, int length) throws BadLocationException {
			super.remove(fb, offs, length);
		}
	}
	
	class UpdateAction implements ActionListener {

		@Override
		public void actionPerformed (ActionEvent arg0) {
			updateOutput();
		}
		
	}
	
	class UpdateChange implements ChangeListener {

		@Override
		public void stateChanged (ChangeEvent e) {
			updateOutput();
		}
	}
	
	class AtlasPanel extends JComponent {
		final int CHECKSIZE = 16;
		
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setColor(Color.BLACK);
			g.clearRect(0, 0, getWidth(), getHeight());
			
			BufferedImage atlasImage = atlasCache!=null ? atlasCache.page() : null;
			
			if (atlasImage!=null) {
				g.clipRect(0, 0, atlasImage.getWidth(), atlasImage.getHeight());
				
				if (background == BGStyle.CheckerLight || background == BGStyle.CheckerDark) {
					Color c1 = background == BGStyle.CheckerLight ? Color.LIGHT_GRAY : Color.DARK_GRAY;
					Color c2 = background == BGStyle.CheckerLight ? Color.WHITE : Color.BLACK;
					for (int x=0; x<(atlasImage.getWidth()/CHECKSIZE + 1); x++) {
						for (int y=0; y<atlasImage.getHeight()/CHECKSIZE + 1; y++) {
							g.setColor( ((x + y) % 2 == 0) ? c1 : c2 );
							g.fillRect(x * CHECKSIZE, y * CHECKSIZE, CHECKSIZE, CHECKSIZE);
						}
					}
				} else {
					g.setColor(background.rgb);
					g.fillRect(0, 0, atlasImage.getWidth(), atlasImage.getHeight());
				}
				
				g.drawImage(atlasImage, 0, 0, null);
			}
		}
	}
	
	class ShadowEditDialog extends JDialog {

		private boolean success = false;
		JSpinner offX, offY, spinRadius, spinIterations;
		JSpinner sliderSpinner;
		
		public ShadowEditDialog(Frame parent) {
			super(parent, false);
			setTitle("Shadow Settings");
			
			Table root = new Table();
			
//			public boolean glow = false;
//			public int glowOffsetX;
//			public int glowOffsetY;
//			public int glowBlurRadius;
//			public int glowBlurIterations;
//			public Color glowColor = new Color(0f, 0f, 0f, 1f);
			
			
			final int HPAD = 5;
			final int SPIN_WIDTH = 85;
			root.pad(5);
			
			spinRadius = new JSpinner(new SpinnerNumberModel(1, 0, MAX_SPINNER_VALUE, 1));
			spinIterations = new JSpinner(new SpinnerNumberModel(1, 0, MAX_SPINNER_VALUE, 1));
			spinRadius.addChangeListener(new UpdateChange());
			spinIterations.addChangeListener(new UpdateChange());
			
			root.addCell("Blur Radius:").right().padRight(HPAD);
			root.addCell(spinRadius).width(SPIN_WIDTH).left();
			root.row();
			
			root.addCell("Blur Iterations:").right().padRight(HPAD);
			root.addCell(spinIterations).width(SPIN_WIDTH).left();
			root.row();
			
			root.addCell("Offset:").right().padRight(HPAD);
			
			offX = new JSpinner(new SpinnerNumberModel(1, -MAX_SPINNER_VALUE, MAX_SPINNER_VALUE, 1));
			offY = new JSpinner(new SpinnerNumberModel(1, -MAX_SPINNER_VALUE, MAX_SPINNER_VALUE, 1));
			offX.setEditor(new JSpinner.NumberEditor(offX, "#"));
			offY.setEditor(new JSpinner.NumberEditor(offY, "#"));
			offX.addChangeListener(new UpdateChange());
			offY.addChangeListener(new UpdateChange());
			
			Table sizeTable = new Table();
			sizeTable.left();
			sizeTable.addCell(offX).left().width(SPIN_WIDTH);
			sizeTable.addCell("x").padLeft(3).padRight(3).center();
			sizeTable.addCell(offY).left().width(SPIN_WIDTH);
			root.addCell(sizeTable).expandX().fillX().row();
			
			final JSlider slider = new JSlider(0, 100, 75);
			sliderSpinner = new JSpinner(new SpinnerNumberModel(0.75, 0.0, 1.0, 0.01));
			sliderSpinner.setEditor(new JSpinner.NumberEditor(sliderSpinner, "0.00"));
			sliderSpinner.addChangeListener(new ChangeListener() {
				
				@Override
				public void stateChanged (ChangeEvent e) {
					double d = ((Number)sliderSpinner.getValue()).doubleValue();
					slider.setValue((int)(d * 100));
					if (!slider.getValueIsAdjusting())
						updateOutput();
				}
			});
			
			slider.addChangeListener(new ChangeListener() {
				
				@Override
				public void stateChanged (ChangeEvent arg0) {
					double p = slider.getValue() / 100.0;
					sliderSpinner.setValue(p);
					if (!slider.getValueIsAdjusting())
						updateOutput();
				}
			});
			
			

			Table sliderTable = new Table();
			sliderTable.left();
			sliderTable.addCell(sliderSpinner).width(SPIN_WIDTH);
			sliderTable.addCell(slider).expandX().fillX();
			
			root.addCell("Opacity:").right().padRight(HPAD);
			root.addCell(sliderTable).expandX().fillX().left();
			
			root.row();
			
			JButton closeBtn = new JButton("Close");
			closeBtn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed (ActionEvent arg0) {
					setVisible(false);
				}
			});
			root.addCell(closeBtn).colspan(2).right().padTop(5);
			
			
			setContentPane(root);
			
			
			pack();
			setSize(350, getHeight());
			setLocationRelativeTo(parent);
		}
		
		public void showEdit() {
			
			setVisible(true);
		}
	}
	
	abstract class AbstractDialog extends JDialog {

		protected boolean success = false;
		
		AbstractDialog(Frame parent, boolean modal) {
			super(parent, modal);
			

			ActionListener successAction = new ActionListener() {
				
				@Override
				public void actionPerformed (ActionEvent arg0) {
					success = true;
					setVisible(false);
				}
			};
			
			JButton okBtn = new JButton("OK");
			okBtn.addActionListener(successAction);
			
			JButton cancelBtn = new JButton("Cancel");
			cancelBtn.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed (ActionEvent arg0) {
					success = false;
					setVisible(false);
				}
			});
			
			Table content = new Table();
			content.pad(10);
			
			Table form = new Table();
			form.top().left();
			
			int w = setup(form, successAction);
			
			Table btnPanel = new Table();
			btnPanel.right();
			btnPanel.addCell(okBtn);
			btnPanel.addCell(cancelBtn);
			
			content.addCell(form).expand().fill();
			content.row();
			content.addCell(btnPanel).padTop(5).right().expandX().fillX();
			
			setContentPane(content);
			pack();
			setSize(w, getHeight());
		}
		
		protected abstract int setup(Table form, ActionListener successAction);
	}
	
	class FontEditDialog extends AbstractDialog {
		
		private JTextField pathField, keyField;
		private JButton pathButton;
		
		public FontEditDialog(Frame parent) {
			super(parent, true);
		}
		
		protected int setup(Table form, ActionListener successAction) {
			pathField = new JTextField();
			pathButton = new IconButton(this, "/data/folder-2.png");
			pathField.addActionListener(successAction);
			
			pathButton.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed (ActionEvent arg0) {
					File f = fileChooser.browse(BrowseType.Open, FileType.FreeType, PrefType.FontLoad, false, null);
					if (f!=null) {
						String fName = f.getName();
						int idx = fName.lastIndexOf('.');
						if (idx>0)
							fName = fName.substring(0, idx);
						keyField.setText(fName);
						pathField.setText( f.getAbsolutePath() );
					}
				}
			});
			
			keyField = new JTextField();
			keyField.addActionListener(successAction);
			form.addCell("Key:").padRight(5).right();
			form.addCell(keyField).colspan(2).expandX().fillX();
			form.row();
			form.addCell("Font Path:").padRight(5).right();
			form.addCell(pathField).expandX().fillX();
			form.addCell(pathButton);
			
			return 350;
		}
		
		public FontItem showFont(FontItem item) {
			success = false;
			if (item==null) {
				setTitle("Add Font");
				pathField.setText("");
				keyField.setText("");
			} else {
				setTitle("Edit Font");
				pathField.setText(item.path);
				keyField.setText(item.key);
			}
			setLocationRelativeTo(getParent());
			setVisible(true);
			
			if (pathField.getText().length()==0 && keyField.getText().length()==0)
				return null;
			
			//if OK was pressed
			if (success) {
				if (item==null) { //we need to return a new item
					String path = pathField.getText();
					return new FontItem(keyField.getText(), path);
				} else  { //return the edited font item
					item.key = keyField.getText();
					item.path = pathField.getText();
					return item;
				}
			}
			return null;
		}
	}
	
	File fontItemFile(FontItem item) {
		if (item.key==null||item.key.length()==0)
			return null;
		
		File f = new File(item.path);
		if (!f.isDirectory() && f.exists())
			return f;
		return null;
	}
	
	class FontCellRenderer extends DefaultListCellRenderer {
		public Component getListCellRendererComponent (JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			
			FontItem item = (FontItem)value;
			if (index>=0 && item!=null) {
				File file = fontItemFile(item);
				
				if (file==null)
					setForeground(Color.RED);
				
				for (int i=0; i<listModel.size(); i++) {
					FontItem oItem = ((FontItem)listModel.get(i));
					String oKey = oItem.key;
					String thisKey = item.key;
					if (oKey!=null && oItem!=item && oKey.equals(thisKey))
						setForeground(Color.RED);
				}
				
				if (item!=null && item == lastInvalidFont)
					setForeground(Color.RED);
			}
			return this;
		}
	}
	
	class EditCharsDialog extends AbstractDialog {
		
		JTextArea charField;
		
		EditCharsDialog(Frame parent) {
			super(parent, true);
			setLocationRelativeTo(parent);
		}
		
		protected int setup(Table form, ActionListener successAction) {
			form.top().left();
			
			charField = new JTextArea(FontPackTool.ABRIDGED_CHARS);
			charField.setWrapStyleWord(false);
			charField.setLineWrap(true);
			
			JScrollPane sp = new JScrollPane(charField);
			
			form.addCell("Characters:").left().padBottom(5).row();
			form.addCell(sp).expand().fill().minSize(100, 100);
			
			final IconButton resetBtn = new IconButton(this, "Reset", "/data/revert.png");
			final IconButton resetBtn2 = new IconButton(this, "Reset (Extended)", "/data/revert.png"); 
			resetBtn.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed (ActionEvent arg0) {
					charField.setText(FontPackTool.ABRIDGED_CHARS);
				}
			});
			resetBtn2.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed (ActionEvent arg0) {
					charField.setText(FontPackTool.DEFAULT_CHARS);
				}
			});
			
			form.row().padTop(2);
			
			Table inner = new Table();
			inner.left();
			inner.addCell(resetBtn).left().row();
			inner.addCell(resetBtn2).left();
			form.addCell(inner).left();
			
			return 400;
		}
		
		private String trimDuplicates(String text) {
			StringBuffer buf = new StringBuffer();
			for (int j=0; j<text.length(); j++) {
				char c = text.charAt(j);
				String str = String.valueOf(c);
				if (buf.indexOf(str) == -1)
					buf.append(str);
			}
			return buf.toString();
		}
		
		public String showEdit(String curChars) {
			charField.setText(curChars);
			setVisible(true);
			if (success) 
				return trimDuplicates(charField.getText());
			return null;
		}
	}
	
	class TestPanel extends Table {

		TestFontPanel panel;
		LwjglCanvas canvas;
		
		public TestPanel() {
			panel = new TestFontPanel(background, FontPackGUI.this);
//			LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
//			config.width = 350;
//			config.height = 350;
//			config.useGL20 = true;
//			config.initialBackgroundColor = com.badlogic.gdx.graphics.Color.BLACK;
			
			canvas = new LwjglCanvas(panel, true);
			addCell(canvas.getCanvas()).minSize(100, 100).expand().fill();
		}
		
		boolean containsCanvas() {
			if (canvas==null)
				return false;
			for (Component c : getComponents()) 
				if (c == canvas.getCanvas())
					return true;
			return false;
		}
		
		public void update() {
			if (canvas!=null)
				panel.update(fontPack, false, background);
		}
		
		public void setRendering(boolean rendering) {
			if (Gdx.graphics!=null)
				Gdx.graphics.setContinuousRendering(rendering);
		}
		
		public boolean isRendering() {
			if (Gdx.graphics!=null)
				return Gdx.graphics.isContinuousRendering();
			else 
				return false;
		}
	}
}
