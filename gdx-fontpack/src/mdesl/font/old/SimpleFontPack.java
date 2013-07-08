package mdesl.font.old;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import com.esotericsoftware.tablelayout.swing.Table;

public class SimpleFontPack extends JFrame {

	class FntFileFilter implements FilenameFilter {

		@Override
		public boolean accept(File f, String s) {
			if (s==null||s.length()==0)
				return false;
			s = s.toLowerCase();
			return s.endsWith(".ttf") || s.endsWith(".otf") 
					|| s.endsWith(".ttc") || s.endsWith(".pfa") 
					|| s.endsWith(".pfb") || s.endsWith(".cff")
					|| s.endsWith(".otc") || s.endsWith(".pcf")
					|| s.endsWith(".fnt") || s.endsWith(".pfr")
					|| s.endsWith(".bdf");
		}
	}
	
	public static void main(String[] args) {
		new SimpleFontPack();
	}
	JList fontList;
	JTextField keyField;
	JTextField pathField;
	JButton pathButton;
	JButton addFntBtn, delFntBtn;
	DefaultListModel listModel = new DefaultListModel();
	
	FileDialog fileChooser;
	Preferences prefs = Preferences.userNodeForPackage(SimpleFontPack.class);
	
	Table settingsPanel;
	
	class FontItem {
		String key;
		String path;
		
//		String characters;
		int[] sizes;
		int padding;
		int glowOffsetX;
		int glowOffsetY;
		int glowBlurRadius;
		int glowBlurIterations;
		Color glowColor;
		
		boolean sizeChecked;
		boolean paddingChecked;
		boolean glowOffsetXChecked;
		boolean glowOffsetYChecked;
		boolean glowBlurRadiusChecked;
		boolean glowBlurIterationsChecked;
		boolean glowColorChecked;
		
		public FontItem(String key) {
			this.key = key;
		}
		
		public String toString() {
			return key+(sizes!=null&&sizes.length!=0 ? " "+Arrays.toString(sizes) : "");
		}
	}

	public SimpleFontPack() {
		super("Font Packer");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		setupGUI();
		updateSelection(null);
		
		setSize(800, 600);
		setLocationRelativeTo(null);
		setVisible(true);
//		addFontDialog.setVisible(true);
		
		
	}
	
	JButton iconButton(String resourcePath) {
		JButton btn = new JButton();
		btn.setIcon(new ImageIcon(SimpleFontPack.class.getResource(resourcePath)));
		btn.setContentAreaFilled(false);
		Dimension d = new Dimension(32, 32);
		btn.setRequestFocusEnabled(false);
		btn.setSize(d);
		btn.setPreferredSize(d);
		btn.setMaximumSize(d);
		return btn;
	}
	
	public String getBestFontPath() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("mac") || os.contains("osx")) {
			File f = new File("/Library/Fonts");
			if (f.exists())
				return f.getAbsolutePath();
			//alternate...
			f = new File("/System/Library/Fonts");
			if (f.exists())
				return f.getAbsolutePath();
		} else if (os.contains("win")) {
			File f = new File("C:\\Windows\\Fonts");
			if (f.exists())
				return f.getAbsolutePath();
		}
		String home = System.getProperty("user.home");
		return home;
	}
	
	public File browseFontFile() {
		if (fileChooser == null) {
			fileChooser = new FileDialog(this);
			fileChooser.setFilenameFilter(new FntFileFilter());
		}
		
		try {
			prefs.sync();
		} catch (BackingStoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String lastDir = prefs.get("file.path", null);
		if (lastDir==null) {
			lastDir = getBestFontPath();
		}
		
		File f = new File(lastDir);
		if (!f.exists())
			f = new File("");
		if (!f.isDirectory())
			f = f.getParentFile();
		lastDir = f.getAbsolutePath();
		
		fileChooser.setDirectory(lastDir);
		fileChooser.setMode(FileDialog.LOAD);
		fileChooser.setVisible(true);
		String res = fileChooser.getFile();
		if (res==null)
			return null; //cancelled
		
		
		File ret = new File(fileChooser.getDirectory(), res);
		File dir = ret;
		if (dir.getParentFile()!=null)
			dir = dir.getParentFile();
		prefs.put("file.path", dir.getAbsolutePath());
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	private void setComponentsEnabled(Container c, boolean en) {
	    Component[] components = c.getComponents();
	    for (Component comp: components) {
	        if (comp instanceof Container)
	            setComponentsEnabled((Container)comp, en);
	        comp.setEnabled(en);
	    }
	}
	
	private void updateSettingsEnabledState(boolean enabled) {
	    for (ISetting s : settings)
	    	s.updateEnabled(enabled);
	}
	
	void updateSelection(FontItem item) {
		System.out.println(item);
//		if (item==null) {
//			return;
//		}
		boolean enabled = item!=null;
		setComponentsEnabled(settingsPanel, enabled);
		updateSettingsEnabledState(enabled);
		
		if (item==null) {
			keyField.setText("");
			pathField.setText("");
			
			for (ISetting s : settings)
		    	s.clearState();
			
			defaultSettingsChanged();
			return;
		}
		
		pathField.setText(item.path);
		keyField.setText(item.key);
		
		Padding.set(item.padding);
		Padding.checkBox.setSelected(item.paddingChecked);
		
		Sizes.setSizes(item.sizes);
		Sizes.checkBox.setSelected(item.sizeChecked);
		
		GlowOffsetX.set(item.glowOffsetX);
		GlowOffsetX.checkBox.setSelected(item.glowOffsetXChecked);
		
		GlowOffsetY.set(item.glowOffsetY);
		GlowOffsetY.checkBox.setSelected(item.glowOffsetYChecked);
		
		GlowBlurRadius.set(item.glowBlurRadius);
		GlowBlurRadius.checkBox.setSelected(item.glowBlurRadiusChecked);
		
		GlowBlurIterations.set(item.glowBlurIterations);
		GlowBlurIterations.checkBox.setSelected(item.glowBlurIterationsChecked);
	}
	
	String toKeyName(String name) {
		int i = name.indexOf('.');
		if (i>0)
			name = name.substring(0, i);
		name = name.replace(" ", "_");
		
		if (!Character.isJavaIdentifierStart(name.charAt(0)))
			name = "_"+name;
		
		StringBuffer b = new StringBuffer();
		for (int j=0; j<name.length(); j++) {
			char c = name.charAt(j);
			if (!Character.isJavaIdentifierPart(c))
				b.append('_');
			else
				b.append(c);
		}
		return removeKeyConflicts( b.toString() );
	}
	
	String removeKeyConflicts(String key) {
		for (int i=0; i<listModel.size(); i++) {
			FontItem item = (FontItem)listModel.get(i);
			if (item.key.equals(key)) {
				key = key+"_1";
				i = 0;
			}
		}
		return key;
	}
	
	void setupGUI() {
		Table table = new Table();
		table.top().left();
		
		fontList = new JList(new FontItem[] { new FontItem("Arial") });
		fontList.setModel(listModel);
		
		addFntBtn = iconButton("/data/add.png");
		delFntBtn = iconButton("/data/delete.png");
		delFntBtn.setEnabled(false);
		
		addFntBtn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				int index = fontList.getSelectedIndex();
				
				File file = browseFontFile();
				if (file==null)
					return;
				
				String name = toKeyName( file.getName() );
				
				FontItem fontItem = new FontItem(name);
				fontItem.path = file.getAbsolutePath();
				
				listModel.add(index+1, fontItem);
				fontList.setSelectedIndex(index+1);
				
				//
//				updateItemSettings();
				
				updateSelection(fontItem);
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
//					updateItemSettings();
					updateSelection((FontItem)fontList.getSelectedValue());
				}
			}
		});
		
		fontList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		fontList.addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int i = fontList.getSelectedIndex();
				delFntBtn.setEnabled(i!=-1);
				if (i>=0) {
//					updateItemSettings();
					updateSelection((FontItem)fontList.getSelectedValue());
				}
			}
		});
		
		JScrollPane scroll = new JScrollPane(fontList, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		Table topTable = new Table();
		
		topTable.addCell("Default Settings:").left().top().row().padTop(5);
		topTable.left().top();
		
		Table topSettingsPanel = new Table();
		topSettingsPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		topSettingsPanel.top().left().pad(10);

		Table topLeft = new Table();
		Table topRight = new Table();
		for (int i=0; i<defaultSettings.size(); i++) {
			ISetting s = defaultSettings.get(i);
			
			Table t = (i<=defaultSettings.size()/2) ? topLeft : topRight;
			s.layout(t);
			t.row();
		}
		topSettingsPanel.addCell(topLeft).top().left().expandX().fillX();
		topSettingsPanel.addCell(new JSeparator(JSeparator.VERTICAL)).padLeft(5).padRight(5).expandY().fillY();
		topSettingsPanel.addCell(topRight).top().left().expandX().fillX();
		topTable.addCell(topSettingsPanel).expand().fill().top().left();
		
		Table leftTable = new Table();
		leftTable.addCell("Fonts:").left().row().padTop(5);
		leftTable.addCell(scroll).minWidth(200).expand().fill().row();
		
		
		Table listBtnTable = new Table();
		listBtnTable.addCell(addFntBtn);
		listBtnTable.addCell(delFntBtn);
		
		leftTable.addCell(listBtnTable).left();
		
		Table rightTable = new Table();
		rightTable.addCell("Individual Settings:").left().top().row().padTop(5);
		
		settingsPanel = new Table();
		settingsPanel.top().left();
		
		Table innerSettings = new Table();
		innerSettings.left().pad(5);
		
		keyField = new JTextField();
		keyField.getDocument().addDocumentListener(new KeyDocListener());
		((AbstractDocument)keyField.getDocument()).setDocumentFilter(new KeyFieldFilter());
		innerSettings.addCell("Key");
		innerSettings.addCell(keyField).expandX().fillX().colspan(2);
		innerSettings.row();
		
		pathField = new JTextField();
		pathButton = iconButton("/data/folder-2.png");
		innerSettings.addCell("Path");
		innerSettings.addCell(pathField).expandX().fillX();
		innerSettings.addCell(pathButton);
		innerSettings.row();
		
		JLabel helpLabel = new JLabel("Leave the setting unchecked to use the default value");
		helpLabel.setFont(helpLabel.getFont().deriveFont(9f));
		innerSettings.addCell(helpLabel).center().colspan(3).padTop(10);
		
		settingsPanel.addCell(innerSettings).colspan(2).expandX().fillX().row();
		
		for (ISetting s : settings) {
			s.layout(settingsPanel);
			settingsPanel.row();
		}
		
		rightTable.addCell(settingsPanel).expand().fill();
		
		table.addCell(topTable).left().top().expandX().fillX().colspan(2).row().padTop(15);
//		table.addCell(new JSeparator()).expandX().fillX().colspan(2).padTop(5).padBottom(5).row();
		
		table.addCell(leftTable).left().top().padRight(10).expand().fill();
		table.addCell(rightTable).left().top().expand().fill().minWidth(200);
		
		
		Table root = new Table();
		root.pad(10);
		
		Table output = new Table();
		
		root.addCell(table).expand().fill().maxWidth(500).left();
		root.addCell(output).expand().fill().center();
		
//		table.row();
//		table.addCell(output).size(256, 256);
		
		setContentPane(root);
	}
	
	
	interface ISetting {
		public void layout(Table table);
		public void clearState();
		public void updateEnabled(boolean enabled);
	}
	
	void defaultSettingsChanged() {
		if (!Padding.checked())
			Padding.set(DPadding.get());
		if (!GlowOffsetX.checked())
			GlowOffsetX.set(DGlowOffsetX.get());
		if (!GlowOffsetY.checked())
			GlowOffsetY.set(DGlowOffsetY.get());
		if (!GlowBlurRadius.checked())
			GlowBlurRadius.set(DGlowBlurRadius.get());
		if (!GlowBlurIterations.checked())
			GlowBlurIterations.set(DGlowBlurIterations.get());
		if (!Sizes.checked())
			Sizes.setText(DSizes.getText());
		updateItemSettings();
	}
	
	void updateItemSettings() {
		FontItem item = (FontItem)fontList.getSelectedValue();
		System.out.println("Changing "+(item!=null?item.key:"NULL"));
		if (item!=null) {
			item.padding = Padding.get();
			item.glowOffsetX = GlowOffsetX.get();
			item.glowOffsetY = GlowOffsetY.get();
			item.glowBlurRadius = GlowBlurRadius.get();
			item.glowBlurIterations = GlowBlurIterations.get();
			item.sizes = Sizes.getSizes();
			
			item.paddingChecked = Padding.checked();
			item.glowOffsetXChecked = GlowOffsetX.checked();
			item.glowOffsetYChecked = GlowOffsetY.checked();
			item.glowBlurRadiusChecked = GlowBlurRadius.checked();
			item.glowBlurIterationsChecked = GlowBlurIterations.checked();
			item.sizeChecked = Sizes.checked();
		}
	}
	
	List<ISetting> defaultSettings = new ArrayList<ISetting>();
	SpinnerSetting DWidth = new SpinnerSetting(false, "Atlas Width", 1024, 2, 4096, defaultSettings);
	SpinnerSetting DHeight = new SpinnerSetting(false, "Atlas Height", 1024, 2, 4096, defaultSettings);
	SpinnerSetting DPadding = new SpinnerSetting(false, "Padding", 0, -100, 100, defaultSettings);
	SpinnerSetting DGlowOffsetX = new SpinnerSetting(false, "Glow Offset X", 0, -100, 100, defaultSettings);
	SpinnerSetting DGlowOffsetY = new SpinnerSetting(false, "Glow Offset Y", 0, -100, 100, defaultSettings);
	SpinnerSetting DGlowBlurRadius = new SpinnerSetting(false, "Glow Blur", 0, 0, 20, defaultSettings);
	SpinnerSetting DGlowBlurIterations = new SpinnerSetting(false, "Glow Blur Iterations", 0, 0, 10, defaultSettings);
	SizeFieldSetting DSizes = new SizeFieldSetting(false, true, "Sizes", defaultSettings);
	
	List<ISetting> settings = new ArrayList<ISetting>();
	SpinnerSetting Padding = new SpinnerSetting(true, "Padding", 0, -100, 100, settings);
	SpinnerSetting GlowOffsetX = new SpinnerSetting(true, "Glow Offset X", 0, -100, 100, settings);
	SpinnerSetting GlowOffsetY = new SpinnerSetting(true, "Glow Offset Y", 0, -100, 100, settings);
	SpinnerSetting GlowBlurRadius = new SpinnerSetting(true, "Glow Blur", 0, 0, 20, settings);
	SpinnerSetting GlowBlurIterations = new SpinnerSetting(true, "Glow Blur Iterations", 0, 0, 10, settings);
	SizeFieldSetting Sizes = new SizeFieldSetting(true, true, "Sizes", settings);
	
	class SizeFieldSetting implements ISetting {
		
		public JTextField sizeField;
		JLabel infoLabel;
		
		JLabel label;
		JCheckBox checkBox;
		
		public SizeFieldSetting(boolean useCheckBox, boolean sizeFilter, String name, List<ISetting> settings) {
			sizeField = new JTextField();
			if (sizeFilter)
				((AbstractDocument)sizeField.getDocument()).setDocumentFilter(new SizeFieldFilter());
			sizeField.getDocument().addDocumentListener(new DocumentListener() {
				
				@Override
				public void removeUpdate(DocumentEvent arg0) {
					change();
				}
				
				@Override
				public void insertUpdate(DocumentEvent arg0) {
					change();
				}
				
				@Override
				public void changedUpdate(DocumentEvent arg0) {
					change();
				}
				
				void change() {
//					System.out.println("Blah");
					if (checkBox==null)
						defaultSettingsChanged();
					else
						updateItemSettings();
				}
			});
			if (useCheckBox) {
				checkBox = new JCheckBox(name);
				checkBox.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						if (label!=null)
							label.setEnabled(checkBox.isSelected());
						sizeField.setEnabled(checkBox.isSelected());
						if (!checkBox.isSelected()) //if checkbox is unselected, use default
							defaultSettingsChanged();
						updateItemSettings();
					}
				});
				sizeField.setEnabled(false);
			} else {
				label = new JLabel(name, JLabel.TRAILING);
			}
			
			if (sizeFilter) {
				infoLabel = new JLabel("Separate sizes by commas and/or spaces");
				infoLabel.setFont(infoLabel.getFont().deriveFont(9f));
			}
			
			settings.add(this);
		}
		
		public void clearState() {
			if (checkBox!=null)
				checkBox.setSelected(false);
			sizeField.setText("");
		}
		
		//updates the size box to be enabled based on checkbox state
		public void updateEnabled(boolean enabled) {
			if (checkBox!=null)
				sizeField.setEnabled(enabled && checkBox.isSelected());
		}

		@Override
		public void layout(Table table) {
			if (checkBox!=null)
				table.addCell(checkBox).left();
			else
				table.addCell(label).right();
			table.addCell(sizeField).left().expandX().fillX();
			if (infoLabel!=null) {
				table.row();
				table.addCell(infoLabel).colspan(2).right();
			}
		}
		
		public boolean checked() {
			return checkBox!=null && checkBox.isSelected();
		}
		
		void setText(String text) {
			sizeField.setText(text);
		}
		
		String getText() {
			return sizeField.getText();
		}
		
		void setSizes(int[] sizes) {
			if (sizes==null)
				sizeField.setText("");
			else {
				StringBuffer buf = new StringBuffer();
				for (int s : sizes)
					buf.append(s).append(" ");
				sizeField.setText( buf.toString().trim() );
			}
		}

		int[] getSizes() {
			String[] spl = sizeField.getText().split("[ \t,]+");
			ArrayList<Integer> ar = new ArrayList<Integer>();
			for (String s : spl) {
				try { ar.add( Integer.parseInt(s)); }
				catch (NumberFormatException e) {}
			}
			int[] ret = new int[ar.size()];
			for (int i=0; i<ret.length; i++)
				ret[i] = ar.get(i);
			return ret;
		}
		
	}
	
	class SpinnerSetting implements ISetting {
		
		JSpinner spinner;
		JLabel label;
		JCheckBox checkBox;
		
		public SpinnerSetting(boolean useCheckBox, String name, int value, int min, int max, List<ISetting> settings) {
			spinner = new JSpinner(new SpinnerNumberModel(value, min, max, 1));
			JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "#"); 
			spinner.setEditor(editor);
			
			if (useCheckBox) {
				checkBox = new JCheckBox(name);
				checkBox.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						if (label!=null)
							label.setEnabled(checkBox.isSelected());
						spinner.setEnabled(checkBox.isSelected());
						if (!checkBox.isSelected()) //if checkbox is unselected, use default
							defaultSettingsChanged();
						updateItemSettings();
					}
				});
				spinner.setEnabled(false);
			} else {
				label = new JLabel(name, JLabel.TRAILING);
			}
			
			spinner.addChangeListener(new ChangeListener() {
				
				@Override
				public void stateChanged(ChangeEvent e) {
					if (checkBox==null) //a default setting (no check box) has been changed
						defaultSettingsChanged();
					else
						updateItemSettings();
				}
			});
			
			settings.add(this);
		}

		public void clearState() {
			if (checkBox!=null)
				checkBox.setSelected(false);
			spinner.setValue(0);
		}
		
		//updates the size box to be enabled based on checkbox state
		public void updateEnabled(boolean enabled) {
			if (checkBox!=null)
				spinner.setEnabled(enabled && checkBox.isSelected());
		}
		
		public boolean checked() {
			return checkBox!=null && checkBox.isSelected();
		}
		
		public void set(int value) {
			spinner.setValue(value);
		}
		
		public int get() {
			return ((Number)spinner.getValue()).intValue();
		}
		
		public void layout(Table table) {
			if (checkBox!=null)
				table.addCell(checkBox).left();
			else
				table.addCell(label).right();
			table.addCell(spinner).left().expandX().fillX();
		}
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
	
	int keyConflict(FontItem ignore, String newKey) {
		for (int i=0; i<listModel.size(); i++) {
			FontItem item = (FontItem)listModel.get(i);
			if (item==ignore)
				continue;
			if (item.key.equals(newKey))
				return i;
		}
		return -1;
	}
	
	
	class KeyFieldFilter extends DocumentFilter {
		
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
			//if the new string is valid...
			for (int i=0; i<str.length(); i++) {
				char c = str.charAt(i);
				if (keyField.getText().length()==0)
					if (!Character.isJavaIdentifierStart(c))
						return false;
				else if (!Character.isJavaIdentifierPart(c))
					return false;
			}
			return true;
		}
		
		public void remove(FilterBypass fb, int offs, int length) throws BadLocationException {
			super.remove(fb, offs, length);
		}
	}
	
	class KeyDocListener implements DocumentListener {
		
		@Override
		public void removeUpdate(DocumentEvent arg0) {
			change();
		}
		
		@Override
		public void insertUpdate(DocumentEvent arg0) {
			change();
		}
		
		@Override
		public void changedUpdate(DocumentEvent arg0) {
			change();
		}
		
		void change() {
			FontItem item = (FontItem)fontList.getSelectedValue();
			if (item==null)
				return;
			item.key = keyField.getText();
			fontList.repaint();
			/*
			boolean updateKey = false;
			
			if (item!=null) {
				int ki = keyConflict(item, keyField.getText());
				if (ki >= 0) {
					FontItem conflict = (FontItem) listModel.get(ki);
					JOptionPane.showMessageDialog(SimpleFontPack.this,
							"Key is already used by " + conflict.key, "Error",
							JOptionPane.ERROR_MESSAGE);
					keyField.setText(item.key);
				} else 
					updateKey = true;
			}
			
			if (updateKey)
				item.key = keyField.getText();*/
		}
	}
	
}
