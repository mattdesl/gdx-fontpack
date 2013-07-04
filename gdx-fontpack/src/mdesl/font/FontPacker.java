package mdesl.font;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
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
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.DocumentFilter;

import com.esotericsoftware.tablelayout.swing.Table;

public class FontPacker extends JFrame {

	class FontDef {
		Types type;
		int[] sizes;
		String path;
		String key;
		int pad;
		
		public FontDef(String key, Types type) {
			this.key = key;
			this.type = type;
		}
		
		public String toString() {
			return key+" "+ (sizes!=null&&sizes.length>0 ? Arrays.toString(sizes) : "");
		}
	}
	
	public static void main(String[] args) {
		new FontPacker();
	}
	
	JList fontList;
	AddFontDialog addFontDialog;
	Font[] allFonts;
	String[] fontNames;
//	List<String> fontNamesList = new ArrayList<String>();
	
	public FontPacker() {
		super("Font Packer");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
		allFonts = e.getAllFonts();
		fontNames = new String[allFonts.length];
		for (int i=0; i<allFonts.length; i++) {
			fontNames[i] = allFonts[i].getFontName();
			//fontNamesList.add(allFonts[i].getFontName());
		}
		
		addFontDialog = new AddFontDialog(this);
		setupGUI();
		
		setSize(500, 400);
		setLocationRelativeTo(null);
		setVisible(true);
//		addFontDialog.setVisible(true);
	}
	
	JComboBox typeSelect;
	JLabel typeSelectLabel, keyFieldLabel, pathFieldLabel, sizeFieldLabel;
	JTextField pathField;
	JButton pathButton;
	JTextField keyField;
	Table settingsPanel = new Table();
	Table pathFieldPanel = new Table();
	JSeparator sep1 = new JSeparator();
	JSpinner padSpinner;
	
	JTextField sizeField;
	TextPrompt sizePrompt;
	
	int[] defaultSizes = new int[] { 12, 16 };
	int defaultPadding = 0;
	int lastFontIndex = 0;
	
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
	
	public void setupGUI() {
		Table table = new Table();
		table.top().left().pad(10);
		
		
		fontList = new JList(new Object[] { "Default Settings", new FontDef("Arial", Types.FreeTypeFont) });
		
		fontList.setCellRenderer(new SettingsListRenderer());
		fontList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		fontList.addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting())
					fontChange(fontList.getSelectedIndex());
			}
		});
		
		
		JScrollPane scroll = new JScrollPane(fontList, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

		Table leftTable = new Table();
		leftTable.addCell("Fonts:").left().row().padTop(5);
		leftTable.addCell(scroll).minWidth(200).expand().fill();
		
		Table rightTable = new Table();
		rightTable.addCell("Settings:").left().top().row().padTop(5);
		
		
		typeSelect = new JComboBox(Types.values());
		
		keyField = new JTextField();
		
		pathField = new JTextField();
		pathButton = new JButton("...");
		
		pathFieldPanel.addCell(pathField).expandX().fillX().left();
		pathFieldPanel.addCell(pathButton).width(50);
		
		sep1 = new JSeparator();
		
		sizeField = new JTextField();
		((AbstractDocument)sizeField.getDocument()).setDocumentFilter(new SizeFieldFilter());
		sizePrompt = new TextPrompt("Default: 12, 13, 14", sizeField);
		sizePrompt.changeAlpha(0.5f);
		sizePrompt.changeStyle(Font.ITALIC);
		
		padSpinner = new JSpinner(new SpinnerNumberModel(0, -100, 100, 1));
		
		settingsPanel.setBorder(BorderFactory.createEtchedBorder());
		settingsPanel.top().left().pad(5);
		setupLayout(null);
		
		rightTable.addCell(settingsPanel).expand().fill();
		
		table.addCell(leftTable).left().top().padRight(10).expand().fill();
		table.addCell(rightTable).left().top().expand().fill().minWidth(200);
		
		setContentPane(table);
	}
	
	void fontChange(int index) {
		if (lastFontIndex<=0) {
			updateSettings();
		} else {
			updateFont((FontDef)fontList.getModel().getElementAt(lastFontIndex));
		}
		
		if (index<=0) {
			setupLayout(null);
		} else {
			setupLayout((FontDef)fontList.getSelectedValue());
		}
		
		lastFontIndex = index;
	}
	
	void updateFont(FontDef def) {
		def.key = keyField.getText();
		def.path = pathField.getText();
		def.sizes = strToSizes(sizeField.getText());
	}
	
	void updateSettings() {
		//get the default settings before moving to next font
		if (sizeField.getText().length()!=0)
			defaultSizes = strToSizes(sizeField.getText());
	}
	
	//null type ==> default settings panel
	void setupLayout(FontDef def) {
		settingsPanel.clear();
		
		//if we are working with the DefaultSettings selection
		if (def==null) {
			sizePrompt.setText("");
			sizeField.setText(sizesToStr(defaultSizes));
		} 
		//we are working with a font...
		else {
			settingsPanel.addCell(typeSelectLabel = new JLabel("Type:", JLabel.TRAILING)).right().padRight(5);
			settingsPanel.addCell(typeSelect).expandX().fillX().row();
			typeSelect.setSelectedItem(def.type);
			
			settingsPanel.addCell(keyFieldLabel = new JLabel("Key:", JLabel.TRAILING)).right().padRight(5);
			settingsPanel.addCell(keyField).expandX().fillX().row();
			keyField.setText(def.key);
			
			settingsPanel.addCell(pathFieldLabel = new JLabel("Path:", JLabel.TRAILING)).right().padRight(5);
			settingsPanel.addCell(pathFieldPanel).expandX().fillX().row();
			pathField.setText(def.path);
			
			settingsPanel.addCell(sep1).expandX().fillX().colspan(2).row();
			
			sizePrompt.setText(sizesToStr(defaultSizes));
			sizeField.setText(sizesToStr(def.sizes));
		}
		
		settingsPanel.addCell(sizeFieldLabel = new JLabel("Size(s):", JLabel.TRAILING)).right().padRight(5);
		settingsPanel.addCell(sizeField).expandX().fillX().row();
		
		if (def==null || def.type == Types.FreeTypeFont) {
			settingsPanel.addCell(new JLabel("Padding:", JLabel.TRAILING)).right().padRight(5);
			settingsPanel.addCell(padSpinner).left().row();
			padSpinner.setValue(def!=null ? def.pad : defaultPadding);
		}
		
		settingsPanel.revalidate();
	}
	
	enum Types {
		FreeTypeFont,
		BMFont;
	}
	
	class AddFontDialog extends JDialog {
		
		JComboBox typeSelect;
		JPanel cardPanel;
		CardLayout cardLayout;
		
		JList systemFontsList;
		
		Table freeTypeCard;
		Table bmFontCard;
		
		JTextField keyField;
		
		//FREE TYPE.....
		JTextField ftFontFilePath;
		SizeField ftSizeField;
		
		//BMFONT.....
				
		AddFontDialog(JFrame parent) {
			super(parent);
			setModal(true);
			
			Table table = new Table();
			table.top().left().pad(10);
			
			Table selectTable = new Table();
			selectTable.left().top();
			selectTable.addCell(new JLabel("Type:", JLabel.TRAILING)).right();
			
			typeSelect = new JComboBox(Types.values());
			typeSelect.addItemListener(new ItemListener() {
				
				@Override
				public void itemStateChanged (ItemEvent e) {
					cardLayout.show(cardPanel, e.getItem().toString());
				}
			});
			selectTable.addCell(typeSelect).expandX().fillX();
			
			selectTable.row();
			selectTable.addCell(new JLabel("Key:", JLabel.TRAILING)).right();
			
			keyField = new JTextField();
			
			selectTable.addCell(keyField).expandX().fillX();;
			
			freeTypeCard = new Table();
			freeTypeCard.left().top();
			bmFontCard = new Table();
			bmFontCard.left().top();
			
			//////// FreeTypeFont Card
			
			//path label
			freeTypeCard.addCell(new JLabel("Font File:", JLabel.TRAILING)).right();
			
			//path & button
			ftFontFilePath = new JTextField();
			freeTypeCard.addCell(ftFontFilePath).left().expandX().fillX();
			freeTypeCard.addCell(new JButton("..."));
			freeTypeCard.row();
			
			//size field
			freeTypeCard.addCell(new JLabel("Sizes:", JLabel.TRAILING)).right();
			
			ftSizeField = new SizeField();
			freeTypeCard.addCell(ftSizeField).left().expandX().fillX();
			
			cardLayout = new CardLayout();
			cardPanel = new JPanel(cardLayout);
			cardPanel.add(freeTypeCard, Types.FreeTypeFont.name());
			cardPanel.add(bmFontCard, Types.BMFont.name());
			
			cardLayout.show(cardPanel, Types.FreeTypeFont.name());
			
			table.addCell(selectTable).center().expandX().fillX();;
			table.row();
			table.addCell(new JSeparator(JSeparator.HORIZONTAL)).left().expandX().fillX();
			table.row();
			table.addCell(cardPanel).left().padTop(5).expand().fill();
			
			//Padding, Blur Radius/Iterations, Blur Color, Blur Offset
			
//			pack();
			setSize(350, 250);
			setLocationRelativeTo(parent);
			setContentPane(table);
		}
	}
	
	class SizeField extends JFormattedTextField {
		
		
		
		SizeField() {
			JFormattedTextField.AbstractFormatter fmt = new JFormattedTextField.AbstractFormatter() {
				
				@Override
				public String valueToString(Object value) throws ParseException {
					System.out.println("value to string");
					return null;
				}
				
				@Override
				public Object stringToValue(String text) throws ParseException {
					String[] str = text.split(" ");
					if (str.length<2)
						invalidEdit();
					return null;
				}
			};
			
			setFormatterFactory( new DefaultFormatterFactory(fmt));
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
	
	
	class SettingsListRenderer extends DefaultListCellRenderer {
		Border b = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY);
		Color bg = new Color(0.9f, 0.9f, 0.9f);
		
		public Component getListCellRendererComponent (JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
						
			if (index==0) {
				setFont(getFont().deriveFont(Font.BOLD | Font.ITALIC));
				setBorder(b);
				if (!isSelected && !cellHasFocus)
					setBackground(bg);
			}
			
			return this;
		}
	}
	
	class FontCellRenderer extends DefaultListCellRenderer {
		public Component getListCellRendererComponent (JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
						
			if (index>=0 && index<allFonts.length) {
				Font f = allFonts[index];
//				setFont(f.deriveFont(16f));
//				setAlignmentY(Component.CENTER_ALIGNMENT);
				
			}
			
			return this;
		}
	}
}
