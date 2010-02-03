package net.sf.sveditor.core.scanner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.sf.sveditor.core.db.SVDBItem;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBMacroDef;
import net.sf.sveditor.core.db.SVDBScopeItem;
import net.sf.sveditor.core.db.index.SVDBFileTree;
import net.sf.sveditor.core.log.LogFactory;
import net.sf.sveditor.core.log.LogHandle;

public class FileContextSearchMacroProvider implements IPreProcMacroProvider {
	private Map<String, SVDBMacroDef>	fMacroCache;
	private SVDBFileTree				fContext;
	private boolean						fDebugEnS = false;
	private int							fIndent = 0;
	private LogHandle					fLog;
	
	
	public FileContextSearchMacroProvider() {
		fMacroCache = new HashMap<String, SVDBMacroDef>();
		fLog = LogFactory.getDefault().getLogHandle("FileContextSearchMacroProvider");
	}
	
	public void setFileContext(SVDBFileTree context) {
		fContext = context;
	}

	public void addMacro(SVDBMacroDef macro) {
		if (fMacroCache.containsKey(macro.getName())) {
			fMacroCache.remove(macro.getName());
		}
		fMacroCache.put(macro.getName(), macro);
	}

	public SVDBMacroDef findMacro(String name, int lineno) {
		if (fMacroCache.containsKey(name)) {
			return fMacroCache.get(name);
		} else {
			return searchContext(fContext, name);
		}
	}

	public void setMacro(String key, String value) {
		if (fMacroCache.containsKey(key)) {
			fMacroCache.get(key).setDef(value);
		} else {
			SVDBMacroDef def = new SVDBMacroDef(key, new ArrayList<String>(), value);
			fMacroCache.put(key, def);
		}
	}

	/**
	 * Search the given context for the named macro
	 * 
	 * Strategy is:
	 * - Search the current context for the named macro
	 * - Search the files included in the current context
	 * - Search up the include tree  
	 * @param context
	 * @param key
	 * @return
	 */
	protected SVDBMacroDef searchContext(
			SVDBFileTree 	context,
			String 			key) {
		SVDBMacroDef ret;
		debug_s(indent(fIndent++) + "--> searchContext(" + context.getFilePath() + ", \"" + key + "\")");
		
		if ((ret = fMacroCache.get(key)) == null) {
			if ((ret = searchDown(context, context, key)) == null) {
				for (SVDBFileTree ib : context.getIncludedByFiles()) {
					ret = searchUp(context, ib, context, key);
				}
			}
			
			if (ret != null) {
				if (fMacroCache.containsKey(key)) {
					fMacroCache.remove(key);
				}
				fMacroCache.put(key, ret);
			}
		}

		debug_s(indent(--fIndent) + "<-- searchContext(" + context.getFilePath() + ", \"" + key + "\"");
		return ret;
	}
	
	/**
	 * Search the specified scope and any sub-scopes
	 * 
	 * @param file
	 * @param context
	 * @param key
	 * @return
	 */
	private SVDBMacroDef searchLocal(SVDBFileTree file, SVDBScopeItem context, String key) {
		SVDBMacroDef m = null;
		debug_s(indent(fIndent++) + "--> searchLocal(" + file.getFilePath() + ", \"" + key + "\"");

		for (SVDBItem it : context.getItems()) {
			debug_s("    it=" + it.getName());
			if (it.getType() == SVDBItemType.Macro && it.getName().equals(key)) {
				m = (SVDBMacroDef)it;
			} else if (it instanceof SVDBScopeItem) {
				m = searchLocal(file, (SVDBScopeItem)it, key);
			}
			
			if (m != null) {
				break;
			}
		}
		
		debug_s(indent(--fIndent) + "<-- searchLocal(" + file.getFilePath() + ", \"" + key + "\"");
		return m;
	}
	
	private SVDBMacroDef searchDown(SVDBFileTree boundary, SVDBFileTree context, String key) {
		SVDBMacroDef m = null;
		
		debug_s(indent(fIndent++) + "--> searchDown(" + context.getFilePath() + ", \"" + key + "\")");
		
		if ((m = searchLocal(context, context.getSVDBFile(), key)) == null) {
			for (SVDBFileTree inc : context.getIncludedFiles()) {
				debug_s(indent(fIndent) + "    searching included file \"" + inc.getFilePath() + "\"");
				if (inc.getSVDBFile() == null) {
					fLog.error("[TODO] do lookup of inc file \"" + 
							inc.getFilePath() + "\"");
				} else {
					if ((m = searchDown(boundary, inc, key)) != null) {
						break;
					}
				}
			}
		}
		
		debug_s(indent(--fIndent) + "<-- searchDown(" + context.getFilePath() + ", \"" + key + "\")");
		return m;
	}
	
	/**
	 * Search up the file tree. 
	 * 
	 * @param context - The context to search
	 * @param child   - The file that is included by context
	 * @param key     - 
	 * @return
	 */
	private SVDBMacroDef searchUp(
			SVDBFileTree	boundary,
			SVDBFileTree 	context,
			SVDBFileTree	child,
			String 			key) {
		SVDBMacroDef m = null;
		
		debug_s(indent(fIndent++) + "--> searchUp(" + context.getFilePath() + ", " + key + ")");
		
		if ((m = searchLocal(context, context.getSVDBFile(), key)) == null) {
			for (SVDBFileTree is : context.getIncludedFiles()) {
				
				if (!is.getFilePath().equals(child.getFilePath()) && (is != boundary)) {
					debug_s(indent(fIndent) + "    included file: " + is.getFilePath());
				
					if ((m = searchDown(boundary, is, key)) == null) {
						for (SVDBFileTree ib : context.getIncludedByFiles()) {
							if ((m = searchUp(boundary, ib, context, key)) != null) {
								break;
							}
						}
					}
				} else {
					debug_s(indent(fIndent) + "    SKIP included file: " + is.getFilePath()
							+ " is-boundary: " + (is == boundary));
				}
				
				if (m != null) {
					break;
				}
			}
		}

		debug_s(indent(--fIndent) + "<-- searchUp(" + context.getFilePath() + ", " + key + ")");
		return m;
	}

	private void debug_s(String str) {
		if (fDebugEnS) {
			fLog.debug(str);
		}
	}
	
	private String indent(int ind) {
		String ret = "";
		while (ind-- > 0) {
			ret += "    ";
		}
		return ret;
	}

}