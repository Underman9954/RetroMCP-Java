package org.mcphackers.mcp.tools.fernflower;

import de.fernflower.main.decompiler.BaseDecompiler;
import de.fernflower.main.decompiler.DirectoryResultSaver;
import de.fernflower.main.decompiler.SingleFileSaver;
import de.fernflower.main.extern.IBytecodeProvider;
import de.fernflower.main.extern.IResultSaver;
import de.fernflower.util.InterpreterUtil;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.mcphackers.mcp.MCPConfig;

public class Decompiler implements IBytecodeProvider {
    public DecompileLogger log;

    public Decompiler() {
        this.log = new DecompileLogger();
    }

    public void decompile(String source, String out, String javadocs) throws IOException {
        Map<String, Object> mapOptions = new HashMap<String, Object>();
        mapOptions.put("rbr", "0");
        mapOptions.put("asc", "1");
        mapOptions.put("nco", "1");
        mapOptions.put("jds", javadocs);
        mapOptions.put("ind", MCPConfig.indentionString);

        SaveType saveType = SaveType.FOLDER;
        File destination = new File(out);
	      if (destination.getName().contains(".zip") || destination.getName().contains(".jar")) {
	        saveType = SaveType.FILE;
	
	        if (destination.getParentFile() != null) {
	          destination.getParentFile().mkdirs();
	        }
	      } else {
	        destination.mkdirs();
	      }
        List<File> lstSources = new ArrayList<File>();
        addPath(lstSources, source);

        if (lstSources.isEmpty()) {
            throw new IOException("No sources found");
        }

        BaseDecompiler decompiler = new BaseDecompiler(this, saveType.getSaver().apply(destination), mapOptions, log);
        try {
            for (File source2 : lstSources) {
                decompiler.addSpace(source2, true);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        decompiler.decompileContext();
    }

    private static void addPath(List<File> list, String path) {
        File file = new File(path);
        if (file.exists()) {
            list.add(file);
        }
    }

    @Override
    public byte[] getBytecode(String externalPath, String internalPath) throws IOException {
        File file = new File(externalPath);
        if (internalPath == null) {
            return InterpreterUtil.getBytes(file);
        } else {
            try (ZipFile archive = new ZipFile(file)) {
                ZipEntry entry = archive.getEntry(internalPath);
                if (entry == null) {
                    throw new IOException("Entry not found: " + internalPath);
                }
                return InterpreterUtil.getBytes(archive, entry);
            }
        }
    }

    public enum SaveType {
      FOLDER(DirectoryResultSaver::new),
      FILE(SingleFileSaver::new);

      private final Function<File, IResultSaver> saver;

      SaveType(Function<File, IResultSaver> saver) {

        this.saver = saver;
      }

      public Function<File, IResultSaver> getSaver() {
        return saver;
      }
    }
}