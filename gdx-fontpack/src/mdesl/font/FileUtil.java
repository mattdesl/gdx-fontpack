package mdesl.font;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public class FileUtil {

	JFileChooser jFileChooser;
	FileDialog fileDialog;
	boolean useJFileChooser;
	Frame parent;
	Preferences prefs;
	String lastDir;
	
	public static enum BrowseType {
		Open,
		Save;
	}
	
	public static enum FileType {
		JSON(AWT_JSON, SWING_JSON),
		FreeType(AWT_FREETYPE, SWING_FREETYPE),
		None(null, null);
		
		public final FilenameFilter awtFilter;
		public final FileFilter swingFilter;
		
		FileType(FilenameFilter awtFilter, FileFilter swingFilter) {
			this.awtFilter = awtFilter;
			this.swingFilter = swingFilter;
		}
	}
	
	public static enum DefaultDir {
		Fonts,
		Home;
	}
	
	
	
	
	public FileUtil (Frame parent, boolean useJFileChooser, Preferences prefs) {
		this.prefs = prefs;
		this.parent = parent;
		this.useJFileChooser = useJFileChooser;
		System.setProperty("apple.awt.fileDialogForDirectories", "true");
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
	
	
	/** Called to initialize the file chooser, if it hasn't already been created. 
	 * The file chooser will be lazily created as necessary. */
	public void init() {
		if (useJFileChooser) {
			if (jFileChooser!=null)
				return;
			jFileChooser = new JFileChooser();
		} else {
			if (fileDialog!=null)
				return;
			fileDialog = new FileDialog(parent);
		}
	}
	
	
	public File browse(BrowseType browseType, FileType fileType, DefaultDir defaultDir, boolean dirsOnly, String startingDir) {
		init();
		
		try {
			prefs.sync();
		} catch (BackingStoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		File selFile = null;
		if (startingDir!=null) {
			File f = new File(startingDir);
			
			if (f.isFile()) //
				startingDir = f.getParent();
			else {
				if (!f.exists())
					f.mkdir();
				
				if (dirsOnly) { //make parent dir the starting dir, if it exists
					startingDir = f.getParent();
					selFile = f;
				}
			}
		}
		
		String lastDir = startingDir!=null && startingDir.length()!=0 ? startingDir : prefs.get("file."+browseType.name(), null);
		if (lastDir==null) {
			if (defaultDir==DefaultDir.Fonts) {
				lastDir = getBestFontPath(); 
			} else {
				lastDir = System.getProperty("user.home");
			}
		}
		
		File lastDirFile = new File(lastDir);
		String lastDirFileName = "";
		if (lastDirFile.exists() && lastDirFile.isFile())
			lastDirFileName = lastDirFile.getName();
		
		File file = null;
		
		if (useJFileChooser) {
			jFileChooser.setFileFilter(fileType.swingFilter);
			
			jFileChooser.setCurrentDirectory(new File(lastDir));
			
			if (selFile!=null)
				jFileChooser.setSelectedFile(selFile);
			
			jFileChooser.setFileSelectionMode(dirsOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
			int a = browseType==BrowseType.Open ? jFileChooser.showOpenDialog(parent) : jFileChooser.showSaveDialog(parent);
			if (a==JFileChooser.APPROVE_OPTION) {
				prefs.put("file."+browseType.name(), jFileChooser.getCurrentDirectory().getAbsolutePath());
				file = jFileChooser.getSelectedFile();
			}
		} else {
			System.setProperty("apple.awt.fileDialogForDirectories", Boolean.toString(dirsOnly));
			
			fileDialog.setFilenameFilter(fileType.awtFilter);
			fileDialog.setDirectory(lastDir);
			fileDialog.setFile(lastDirFileName);
			fileDialog.setMode(browseType==BrowseType.Open ? FileDialog.LOAD : FileDialog.SAVE);
			fileDialog.setVisible(true);
			String res = fileDialog.getFile();
			if (res!=null) {
				File ret = new File(fileDialog.getDirectory(), res);
				File dir = ret;
				if (dir.getParentFile()!=null && !dir.isDirectory())
					dir = dir.getParentFile();
				prefs.put("file."+browseType.name(), dir.getAbsolutePath());
				file = ret;
			}
		}
		
		if (file!=null) {
			try {
				prefs.flush();
			} catch (BackingStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
		return file;
	}
	
	static final FilenameFilter AWT_FREETYPE = new FilenameFilter() {
		
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
	};
	
	static final FileFilter SWING_FREETYPE = new FileFilter() {
		
		@Override
		public String getDescription () {
			return "Free Type Font (.ttf, .otf)";
		}
		
		@Override
		public boolean accept (File f) {
			if (f.isDirectory())
				return true;
			String s = f.getName();
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
	};
	
	

	static final FilenameFilter AWT_JSON = new FilenameFilter() {
		
		@Override
		public boolean accept(File f, String s) {
			if (s==null||s.length()==0)
				return false;
			s = s.toLowerCase();
			return s.endsWith(".json");
		}
	};
	
	static final FileFilter SWING_JSON = new FileFilter() {
		
		@Override
		public String getDescription () {
			return "Font Pack (.json)";
		}
		
		@Override
		public boolean accept (File f) {
			if (f.isDirectory())
				return true;
			String s = f.getName();
			if (s==null||s.length()==0)
				return false;
			s = s.toLowerCase();
			return s.endsWith(".json");
		}
	};
	
}
