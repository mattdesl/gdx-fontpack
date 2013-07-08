
package mdesl.font;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import mdesl.font.FileUtil.BrowseType;
import mdesl.font.FileUtil.DefaultDir;
import mdesl.font.FileUtil.FileType;
import mdesl.font.FontPackTool.FontItem;
import mdesl.font.FontPackTool.FontPackDocument;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import com.esotericsoftware.tablelayout.swing.Table;

public class FontPackGUI extends JFrame implements FontPackTool.ProgressListener {
	
	private static boolean useJFileChooser = true;
	
	public static void main(String[] args) {
		new SharedLibraryLoader("libs/gdx-natives.jar").load("gdx");
		new SharedLibraryLoader("libs/gdx-freetype-natives.jar").load("gdx-freetype");
		
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("osx") || osName.contains("mac")) {
			useJFileChooser = false;
		}
		
		new FontPackGUI();
	}
	
	JLabel statusBar;
	ImageIcon iconWarn, iconErr, iconInfo;
	JList fontList;
	JTextField outPathField;
	JSpinner sizeWidth, sizeHeight;
	JSpinner spacingSpinner;
	JTextField sizeField;
	
	JButton outPathButton;
	JButton addFntBtn, delFntBtn, editFntBtn;
	DefaultListModel listModel = new DefaultListModel();
	
	JCheckBox shadowCheckbox;
	JButton shadowButton;
	
	Preferences prefs = Preferences.userNodeForPackage(FontPackGUI.class);
	FileUtil fileChooser;
	
	Table listTable = new Table();
	Table settingsTable = new Table();
	Table outputTable = new Table();
	
	BufferedImage atlasImage;
	int[] atlasImagePixels;
	AtlasPanel atlasPanel = new AtlasPanel();
	
	FontEditDialog fntDiag;

	Table root = new Table();
	JPanel statusPanel;
	
	public FontPackGUI() {
		super("Font Packer");
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception e) { System.out.println("Error setting Java LAF: " + e); }

		fileChooser = new FileUtil(this, useJFileChooser, prefs);
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setupGUI();
		
		updateOutput();
		
		setSize(800, 512);
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
		
		left.addCell(hr("settings")).expandX().fillX().padTop(20).padBottom(5).row();
		left.addCell(settingsTable).expandX().fillX().row();
		
		//GENERATE
		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed (ActionEvent arg0) {
				updateOutput(true);
			}
		});
		left.addCell(new JSeparator()).expandX().fillX().padTop(5).padBottom(5).row();
		left.addCell(saveButton).colspan(2);
		
		
		//output
		atlasPanel = new AtlasPanel();
		Dimension d = new Dimension(512, 512);
		atlasPanel.setPreferredSize(d);
		atlasPanel.setMinimumSize(d);
		
		outputTable.addCell(atlasPanel).expand().fill();
		
		root.addCell(left).fill().expand().left().pad(5).padRight(10);
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
		
		//OUTPUT PATH
		Table outTable = new Table();
		
		outPathField = new JTextField();
		outPathButton = iconButton("/data/folder-2.png");
		
		outPathField.setText( prefs.get("file."+BrowseType.Save, "") );
		
		outPathButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed (ActionEvent arg0) {
				File f = fileChooser.browse(BrowseType.Save, FileType.JSON, DefaultDir.Home, false);
				if (f!=null)
					outPathField.setText(f.getAbsolutePath());
			}
		});
		
		settingsTable.addCell("Output:").right().padRight(HPAD);
		
		outTable.addCell(outPathField).expandX().fillX();
		outTable.addCell(outPathButton);
		
//		outTable.row();
//		JLabel pathInfoLabel = new JLabel("Images and fnt files will be saved to the directory");
//		pathInfoLabel.setFont(pathInfoLabel.getFont().deriveFont(9f));
//		outTable.addCell(pathInfoLabel).right();
		
		
		settingsTable.addCell(outTable).expandX().fillX();
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
		sizeTable.addCell("x").center();
		sizeTable.addCell(sizeHeight).left();
		settingsTable.addCell(sizeTable).expandX().fillX().row();
		
		//SPACING
		spacingSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 100, 1));
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
		shadowCheckbox.addActionListener(new UpdateAction());
		shadowButton = iconButton("/data/pencil.png");
		shadowButton.setEnabled(false);
		
		Table shadowTable = new Table();
		
		settingsTable.row();
		settingsTable.addCell("Shadow:").right().padRight(HPAD);
		shadowTable.addCell(shadowCheckbox);
		shadowTable.addCell(shadowButton);
		settingsTable.addCell(shadowTable).left();
		
		
		//SIZES
		sizeField = new JTextField("12, 16");
		sizeField.addActionListener(new UpdateAction());
		((AbstractDocument)sizeField.getDocument()).setDocumentFilter(new SizeFieldFilter());
		
		settingsTable.row();
		settingsTable.addCell("Sizes:").right().padRight(HPAD);
		settingsTable.addCell(sizeField).expandX().fillX();
		settingsTable.row();
		
		JLabel infoLabel = new JLabel("Separate font sizes by commas and/or spaces");
		infoLabel.setFont(infoLabel.getFont().deriveFont(9f));
		settingsTable.addCell(infoLabel).colspan(2).right();
	}
	
	JSpinner padTop, padLeft, padBottom, padRight;
	
	private Table paddingTable() {
		Table spinnerTable = new Table();
		
		padTop = new JSpinner(new SpinnerNumberModel(0, -100, 100, 1));
		padLeft = new JSpinner(new SpinnerNumberModel(0, -100, 100, 1));
		padBottom = new JSpinner(new SpinnerNumberModel(0, -100, 100, 1));
		padRight = new JSpinner(new SpinnerNumberModel(0, -100, 100, 1));
		
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
		
		FontItem item = new FontItem("Arial", "Arial.ttf");
		listModel.addElement(item);
		
		addFntBtn = iconButton("/data/add.png");
		delFntBtn = iconButton("/data/delete.png");
		delFntBtn.setEnabled(false);
		editFntBtn = iconButton("/data/pencil.png");
		editFntBtn.setEnabled(false);
		
		addFntBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				int index = fontList.getSelectedIndex();
				
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
		
		Table btnTable = new Table();
		btnTable.left();
		
		btnTable.addCell(addFntBtn).size(28);
		btnTable.addCell(delFntBtn).size(28);
		btnTable.addCell(editFntBtn).size(28);
		
		listTable.row();
		listTable.addCell(btnTable).expandX().fillX();
	}
	

	JButton iconButton(String resourcePath) {
		JButton btn = new JButton();
		btn.setIcon(new ImageIcon(FontPackGUI.class.getResource(resourcePath)));
		btn.setContentAreaFilled(false);
		Dimension d = new Dimension(32, 32);
		btn.setRequestFocusEnabled(false);
		btn.setSize(d);
		btn.setPreferredSize(d);
		btn.setMaximumSize(d);
		return btn;
	}

	FontPackDocument doc = new FontPackDocument();
	
	void updateListRenderer() {
		
	}
	
	void updateOutput() {
		updateOutput(false);
	}
	
	void updateOutput(boolean save) {
		status(null, "");
		String path = outPathField.getText();
		if (path.length()==0) {
			System.out.println("No path");//todo: fixme
			return;
		}
		FileHandle outFile = new FileHandle(path);
		
		FileHandle outDir = outFile.isDirectory() ? outFile : outFile.parent();
		String imageOutName = outFile.isDirectory() ? "fonts" : outDir.nameWithoutExtension();
		
		doc.atlasWidth = ((Number)sizeWidth.getValue()).intValue();
		doc.atlasHeight = ((Number)sizeHeight.getValue()).intValue();
		doc.spacing = ((Number)spacingSpinner.getValue()).intValue();
		doc.defaultSettings.characters = FontPackTool.ABRIDGED_CHARS;
		
		doc.defaultSettings.glow = shadowCheckbox.isSelected();
		doc.defaultSettings.glowOffsetX = 1;
		doc.defaultSettings.glowOffsetY = 1;
		doc.defaultSettings.glowBlurRadius = 1;
		doc.defaultSettings.glowBlurIterations = 1;
		
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
			PixmapPacker packer = FontPackTool.pack(doc, outDir, imageOutName);
			onAtlasChanged(packer.getPages().get(0).getPixmap());
			
			packer.dispose();
		} catch (Exception e) {
			status(iconErr, e.getMessage());
			
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
	}
	
	public void onAtlasChanged(Pixmap pix) {
		int w = pix.getWidth();
		int h = pix.getHeight();
		
		if (atlasImage==null || w!=atlasImage.getWidth() || h!=atlasImage.getHeight()) {
			atlasImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			atlasImagePixels = ((DataBufferInt)atlasImage.getData().getDataBuffer()).getData();
		}
		toARGB(pix, atlasImagePixels);
		
		atlasImage.setRGB(0,  0,  w, h, atlasImagePixels, 0, w);
		
		Dimension d = new Dimension(512, 512);
		atlasPanel.setPreferredSize(d);
		atlasPanel.setMinimumSize(d);
		
		outputTable.revalidate();
		outputTable.repaint();
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
		Set<Integer> ar = new HashSet<Integer>();
		for (String s : spl) {
			try {
				int i = Integer.parseInt(s);
				if (i>1)
					ar.add( i ); 
			} catch (NumberFormatException e) {}
		}
		int[] ret = new int[ar.size()];
		int idx = 0;
		for (int el : ar) {
			ret[idx++] = el;
		}
		return ret;
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
			g.fillRect(0, 0, getWidth(), getHeight());
				
			if (atlasImage!=null) {
				for (int x=0; x<(atlasImage.getWidth()/CHECKSIZE + 1); x++) {
					for (int y=0; y<atlasImage.getHeight()/CHECKSIZE + 1; y++) {
						g.setColor( ((x + y) % 2 == 0) ? Color.DARK_GRAY : Color.BLACK );
						g.fillRect(x * CHECKSIZE, y * CHECKSIZE, CHECKSIZE, CHECKSIZE);
					}
				}
				
				g.drawImage(atlasImage, 0, 0, null);
			}
		}
	}
	
	class FontEditDialog extends JDialog {
		
		private boolean success = false;
		private JTextField pathField, keyField;
		private JButton pathButton;
		
		public FontEditDialog(Frame parent) {
			super(parent);
			setModal(true);
			
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
			
			pathField = new JTextField();
			pathButton = iconButton("/data/folder-2.png");
			pathField.addActionListener(successAction);
			
			keyField = new JTextField();
			keyField.addActionListener(successAction);
			form.addCell("Key:").padRight(5).right();
			form.addCell(keyField).colspan(2).expandX().fillX();
			form.row();
			form.addCell("Font Path:").padRight(5).right();
			form.addCell(pathField).expandX().fillX();
			form.addCell(pathButton);
//			form.addCell();
			
			Table btnPanel = new Table();
			btnPanel.right();
			btnPanel.addCell(okBtn);
			btnPanel.addCell(cancelBtn);
			
			content.addCell(form).expand().fill();
			content.row();
			content.addCell(btnPanel).right().expandX().fillX();
			
			setContentPane(content);
			pack();
			setSize(350, getHeight());
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
				
			boolean invalid = false;
			FontItem item = (FontItem)value;
			if (index>=0 && item!=null) {
				File file = fontItemFile(item);
				
				if (file==null)
					setForeground(Color.RED);
			}
			return this;
		}
	}
}
