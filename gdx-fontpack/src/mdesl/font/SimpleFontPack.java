package mdesl.font;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
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

	public static void main(String[] args) {
		new SimpleFontPack();
	}
	JList fontList;
	JTextField keyField;
	JTextField pathField;
	JButton pathButton;
	
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
		
		setSize(500, 500);
		setLocationRelativeTo(null);
		setVisible(true);
//		addFontDialog.setVisible(true);
	}
	
	void setupGUI() {
		Table table = new Table();
		table.top().left().pad(10);
		
		fontList = new JList(new FontItem[] { new FontItem("Arial") });
		
		fontList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		fontList.addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				
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
		leftTable.addCell(scroll).minWidth(200).expand().fill();
		
		Table rightTable = new Table();
		rightTable.addCell("Individual Settings:").left().top().row().padTop(5);
		
		Table settingsPanel = new Table();
		settingsPanel.top().left();
		
		Table innerSettings = new Table();
		innerSettings.left().pad(5);
		
		keyField = new JTextField();
		innerSettings.addCell("Key");
		innerSettings.addCell(keyField).expandX().fillX().colspan(2);
		innerSettings.row();
		
		pathField = new JTextField();
		pathButton = new JButton("...");
		innerSettings.addCell("Path");
		innerSettings.addCell(pathField).expandX().fillX();
		innerSettings.addCell(pathButton).width(50);
		
		settingsPanel.addCell(innerSettings).colspan(2).expandX().fillX().row();
		
		for (ISetting s : settings) {
			s.layout(settingsPanel);
			settingsPanel.row();
		}
		
		rightTable.addCell(settingsPanel).expand().fill();
		
		table.addCell(topTable).left().top().expandX().fillX().colspan(2).row().padTop(15);
		table.addCell(new JSeparator()).expandX().fillX().colspan(2).padTop(5).padBottom(5).row();
		
		table.addCell(leftTable).left().top().padRight(10).expand().fill();
		table.addCell(rightTable).left().top().expand().fill().minWidth(200);
		
		setContentPane(table);
		
	}
	
	int nextPOT(int i) {
		
		int val = i; // Get input
		val--;
		val = (val >> 1) | val;
		val = (val >> 2) | val;
		val = (val >> 4) | val;
		val = (val >> 8) | val;
		val = (val >> 16) | val;
		return val++; // Val is now the next highest power of 2.

	}
	
	interface ISetting {
		public void layout(Table table);
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
		if (item!=null) {
			item.padding = Padding.get();
			item.glowOffsetX = GlowOffsetX.get();
			item.glowOffsetY = GlowOffsetY.get();
			item.glowBlurRadius = GlowBlurRadius.get();
			item.glowBlurIterations = GlowBlurIterations.get();
			item.sizes = Sizes.getSizes();
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
						else
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
						else
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
	
	
}
