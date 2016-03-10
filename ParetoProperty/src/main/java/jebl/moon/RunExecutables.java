package jebl.moon;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;

import jebl.evolution.io.NewickImporter;
import jebl.evolution.trees.RootedTree;

public class RunExecutables {
	public static RootedTree dupTreeV2(File input, long timeout) throws Exception {
		String [] cmd = {
				"", "-i", ""
		};

		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win"))
			cmd[0] = "executables/duptree2.exe";
		else if (os.contains("mac"))
			cmd[0] = "executables/duptree2.macosx";
		else
			cmd[0] = "executables/duptree2.linux64";
		cmd[2] = input.getAbsolutePath();

		ProcessBuilder pb = new ProcessBuilder();
		pb.redirectErrorStream(true);				
		pb.command(cmd);				
		Process p = pb.start();		
		
		BufferedReader stdin = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		long begin = System.currentTimeMillis(), exec = 0;
		boolean found = false;
		RootedTree tree = null;
		while ((line = stdin.readLine()) != null) {
			exec = System.currentTimeMillis() - begin;
			if (exec >= timeout * 1000) {
				p.destroyForcibly();
			}
			if (line.startsWith("[ species tree ]")) {
				found = true;			
			}
			else if (found) {
				NewickImporter newick = new NewickImporter(new StringReader(line), true);								
				tree = (RootedTree) newick.importNextTree();					
				p.destroyForcibly();
				break;
			}
		}			
		stdin.close();
		return tree;
	}
	
	public static RootedTree dupTreeV1(File input, long timeout) throws Exception {
		String [] cmd = {
				"", "-i", ""
		};

		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win"))
			cmd[0] = "executables/duptree.exe";
		else if (os.contains("mac"))
			cmd[0] = "executables/duptree.macosx";
		else
			cmd[0] = "executables/duptree.linux64";
		cmd[2] = input.getAbsolutePath();

		ProcessBuilder pb = new ProcessBuilder();
		pb.redirectErrorStream(true);				
		pb.command(cmd);				
		Process p = pb.start();		
		
		BufferedReader stdin = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		long begin = System.currentTimeMillis(), exec = 0;
		boolean found = false;
		RootedTree tree = null;
		while ((line = stdin.readLine()) != null) {
			exec = System.currentTimeMillis() - begin;
			if (exec >= timeout * 1000) {
				p.destroyForcibly();
			}
			if (line.startsWith("Found species tree")) {
				found = true;			
			}
			else if (found) {
				NewickImporter newick = new NewickImporter(new StringReader(line), true);								
				tree = (RootedTree) newick.importNextTree();					
				p.destroyForcibly();
				break;
			}
		}			
		stdin.close();
		return tree;
	}
}
