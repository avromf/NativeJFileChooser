/*
 * Copyright (c) 2018, Steffen Flor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package li.flor.nativejfilechooser;

import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * This is a drop-in replacement for Swing's file chooser.<p>
 * Instead of displaying Swing's file chooser, it makes use of JavaFX's file chooser. JavaFX uses the OS's native file
 * chooser. Technically, this class is a memory hog, but its use is convenient. Furthermore, if JavaFX is not available,
 * the default file chooser will be displayed instead. Of course, this class will not compile if you don't have an JDK 8
 * or higher that has JavaFX support. Since this class will have to call the constructor of JFileChooser, it won't
 * increase the performance of the file chooser; if anything, it might further decrease it. Please note that some
 * methods have not been overwritten and may not have any impact on the file chooser. Sometimes, the new JavaFX file
 * chooser does not provide certain functionality. One feature that is not supported is the selection of files AND
 * directories. If trying to set this using setFileSelectionMode(), still only files will be selectable.
 *
 * @author Steffen Flor
 * @version 1.6.4
 */
public class NativeJFileChooser extends JFileChooser {

	public static final boolean	FX_AVAILABLE;
	protected List<File>				currentFiles;
	protected FileChooser			fileChooser;
	protected DirectoryChooser		directoryChooser;
	protected File						currentFile;
	protected File						currentDirectory;
	
    static {
        boolean isFx;
        try {
            Class.forName("javafx.stage.FileChooser");
            isFx = true;
            // Initializes JavaFX environment
            JFXPanel jfxPanel = new JFXPanel();
        } catch (ClassNotFoundException e) {
            isFx = false;
        }

        FX_AVAILABLE = isFx;
    }

    public NativeJFileChooser() {
        initFxFileChooser(null);
    }

    public NativeJFileChooser(String currentDirectoryPath) {
		super();
		File dir = new File(currentDirectoryPath);
		initFxFileChooser(dir);
		setCurrentDirectory(dir);
    }

    public NativeJFileChooser(File currentDirectory) {
        super();
        initFxFileChooser(currentDirectory);
        setCurrentDirectory(currentDirectory);
    }

    public NativeJFileChooser(FileSystemView fsv) {
        super(fsv);
        initFxFileChooser(fsv.getDefaultDirectory());
    }

    public NativeJFileChooser(File currentDirectory, FileSystemView fsv) {
		super(fsv);
		initFxFileChooser(currentDirectory);
		setCurrentDirectory(currentDirectory);
    }

    public NativeJFileChooser(String currentDirectoryPath, FileSystemView fsv) {
		super(fsv);
		File dir = new File(currentDirectoryPath);
		initFxFileChooser(dir);
		setCurrentDirectory(dir);
    }

    @Override
    public int showOpenDialog(final Component parent) throws HeadlessException {
        if (!FX_AVAILABLE) {
            return super.showOpenDialog(parent);
        }

        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                if (parent != null) {
                    parent.setEnabled(false);
                }

                if (isDirectorySelectionEnabled()) {
                    currentFile = directoryChooser.showDialog(null);
                } else {
                    if (isMultiSelectionEnabled()) {
                        currentFiles = fileChooser.showOpenMultipleDialog(null);
                    } else {
                        currentFile = fileChooser.showOpenDialog(null);
                    }
                }
                latch.countDown();
            }

        });
        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (parent != null) {
                parent.setEnabled(true);
            }
        }

        if (isMultiSelectionEnabled()) {
            if (currentFiles != null) {
                return JFileChooser.APPROVE_OPTION;
            } else {
                return JFileChooser.CANCEL_OPTION;
            }
        } else {
            if (currentFile != null) {
                return JFileChooser.APPROVE_OPTION;
            } else {
                return JFileChooser.CANCEL_OPTION;
            }
        }

    }

    @Override
    public int showSaveDialog(final Component parent) throws HeadlessException {
        if (!FX_AVAILABLE) {
            return super.showSaveDialog(parent);
        }

        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                if (parent != null) {
                    parent.setEnabled(false);
                }

                if (isDirectorySelectionEnabled()) {
                    currentFile = directoryChooser.showDialog(null);
                } else {
                    currentFile = fileChooser.showSaveDialog(null);
                }
                latch.countDown();
            }

        });
        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (parent != null) {
                parent.setEnabled(true);
            }
        }

        if (currentFile != null) {
            return JFileChooser.APPROVE_OPTION;
        } else {
            return JFileChooser.CANCEL_OPTION;
        }
    }

    @Override
    public int showDialog(Component parent, String approveButtonText) {
        if (!FX_AVAILABLE) {
            return super.showDialog(parent, approveButtonText);
        }
        return showOpenDialog(parent);
    }

    @Override
    public File[] getSelectedFiles() {
        if (!FX_AVAILABLE) {
            return super.getSelectedFiles();
        }
        if (currentFiles == null) {
            return null;
        }
        return currentFiles.toArray(new File[currentFiles.size()]);
    }

    @Override
    public File getSelectedFile() {
        if (!FX_AVAILABLE) {
            return super.getSelectedFile();
        }
        if (isMultiSelectionEnabled()) {
      	  if (currentFiles != null && !currentFiles.isEmpty())
      		  return currentFiles.get(0);
      	  else
      		  return null;
        }
        else
      	  return currentFile;
    }

    @Override
    public void setSelectedFiles(File[] selectedFiles) {
        if (!FX_AVAILABLE) {
            super.setSelectedFiles(selectedFiles);
            return;
        }
        if (selectedFiles == null || selectedFiles.length == 0) {
            currentFiles = null;
        } else {
            setSelectedFile(selectedFiles[0]);
            currentFiles = new ArrayList<>(Arrays.asList(selectedFiles));
        }
    }

    @Override
    public void setSelectedFile(File file) {
        if (!FX_AVAILABLE) {
            super.setSelectedFile(file);
            return;
        }
        currentFile = file;
        if (file != null) {
            if (file.isDirectory()) {
                fileChooser.setInitialDirectory(file.getAbsoluteFile());

                if (directoryChooser != null) {
                    directoryChooser.setInitialDirectory(file.getAbsoluteFile());
                }
            } else {
                File parentFile = file.getParentFile();
                if (parentFile != null && parentFile.isDirectory())
               	 fileChooser.setInitialDirectory(parentFile);
                
                fileChooser.setInitialFileName(file.getName());

                if (directoryChooser != null && parentFile != null && parentFile.isDirectory()) {
                    directoryChooser.setInitialDirectory(parentFile);
                }
            }

        }
    }
    
    @Override
   public void setCurrentDirectory(File dir)
   {
   	 if (!FX_AVAILABLE) {
   		 super.setCurrentDirectory(dir);
   		 return;
   	 }
   	 currentDirectory = dir;
   	 if (dir != null) {
          if (dir.isDirectory()) {
              fileChooser.setInitialDirectory(dir.getAbsoluteFile());

              if (directoryChooser != null) {
                  directoryChooser.setInitialDirectory(dir.getAbsoluteFile());
              }
          }
   	 }
   }

    @Override
    public void setFileSelectionMode(int mode) {
        super.setFileSelectionMode(mode);
        if (!FX_AVAILABLE) {
            return;
        }
        if (mode == DIRECTORIES_ONLY) {
            if (directoryChooser == null) {
                directoryChooser = new DirectoryChooser();
            }
            setCurrentDirectory(currentDirectory);
            // Set file again, so directory chooser will be affected by it
            setSelectedFile(currentFile);
            setDialogTitle(getDialogTitle());
        }
    }

    @Override
    public void setDialogTitle(String dialogTitle) {
        if (!FX_AVAILABLE) {
            super.setDialogTitle(dialogTitle);
            return;
        }
        fileChooser.setTitle(dialogTitle);
        if (directoryChooser != null) {
            directoryChooser.setTitle(dialogTitle);
        }
    }

    @Override
    public String getDialogTitle() {
        if (!FX_AVAILABLE) {
            return super.getDialogTitle();
        }
        return fileChooser.getTitle();
    }

    @Override
    public void changeToParentDirectory() {
        if (!FX_AVAILABLE) {
            super.changeToParentDirectory();
            return;
        }
        File parentDir = fileChooser.getInitialDirectory().getParentFile();
        if (parentDir.isDirectory()) {
            fileChooser.setInitialDirectory(parentDir);
            if (directoryChooser != null) {
                directoryChooser.setInitialDirectory(parentDir);
            }
        }
    }

    @Override
    public void addChoosableFileFilter(FileFilter filter) {
        super.addChoosableFileFilter(filter);
        if (!FX_AVAILABLE || filter == null) {
            return;
        }
        if (filter.getClass().equals(FileNameExtensionFilter.class)) {
            FileNameExtensionFilter f = (FileNameExtensionFilter) filter;

            List<String> ext = new ArrayList<>();
            for (String extension : f.getExtensions()) {
                ext.add(extension.replaceAll("^\\*?\\.?(.*)$", "*.$1"));
            }
            FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter(f.getDescription(), ext);
				if (!containsFilter(fileChooser.getExtensionFilters(), extensionFilter))
					fileChooser.getExtensionFilters().add(extensionFilter);
        }
    }
    
    @Override
   public void setFileFilter(FileFilter filter)
   {
   	 if (!FX_AVAILABLE) {
   		 super.setFileFilter(filter);
          return;
      }
   	 if (filter.getClass().equals(FileNameExtensionFilter.class)) {
          FileNameExtensionFilter f = (FileNameExtensionFilter) filter;

          List<String> ext = new ArrayList<>();
          for (String extension : f.getExtensions()) {
              ext.add(extension.replaceAll("^\\*?\\.?(.*)$", "*.$1"));
          }
          
			FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter(f.getDescription(), ext);
			if (!containsFilter(fileChooser.getExtensionFilters(), extensionFilter))
				fileChooser.getExtensionFilters().add(extensionFilter);
			
         fileChooser.setSelectedExtensionFilter(extensionFilter);
      }
   	
   }
    
	private boolean containsFilter(ObservableList<ExtensionFilter> extensionFilters, ExtensionFilter extensionFilter)
	{
		return extensionFilters.stream().anyMatch(f -> matchFilter(extensionFilter, f));
	}

	private boolean matchFilter(ExtensionFilter extensionFilter, ExtensionFilter compareFilter)
	{
		if (!extensionFilter.getDescription().equals(compareFilter.getDescription()))
			return false;
		else
			return extensionFilter.getExtensions().equals(compareFilter.getExtensions());
	}

	@Override
    public void setAcceptAllFileFilterUsed(boolean bool) {
        boolean differs = isAcceptAllFileFilterUsed() ^ bool;
        super.setAcceptAllFileFilterUsed(bool);
        if (!FX_AVAILABLE) {
            return;
        }
        if (!differs) {
            return;
        }
        if (bool) {
            fileChooser.getExtensionFilters()
                    .add(new FileChooser.ExtensionFilter("All files", "*.*"));
        } else {
            for (Iterator<FileChooser.ExtensionFilter> it
                    = fileChooser.getExtensionFilters().iterator(); it.hasNext();) {
                FileChooser.ExtensionFilter filter = it.next();
                if (filter.getExtensions().size() == 1
                        && filter.getExtensions().contains("*.*")) {
                    it.remove();
                }
            }
        }

    }

    private void initFxFileChooser(File currentFile) {
        if (FX_AVAILABLE) {
            fileChooser = new FileChooser();
            this.currentFile = currentFile;
            setSelectedFile(currentFile);
            fileChooser.getExtensionFilters()
            	.add(new FileChooser.ExtensionFilter("All files", "*.*"));
        }
    }

}
