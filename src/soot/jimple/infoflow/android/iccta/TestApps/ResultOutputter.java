package soot.jimple.infoflow.android.iccta.TestApps;

import java.util.ArrayList;
import java.util.List;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;

public class ResultOutputter
{
	static List<List<Unit>> stmtsList = new ArrayList<List<Unit>>();
	static List<List<Unit>> iccStmtsList = new ArrayList<List<Unit>>();
	
	public static void output(IInfoflowCFG cfg, InfoflowResults results) 
	{
		try
		{
			if (null != results && null != results.getResults() && ! results.getResults().isEmpty())
			{	
				for (ResultSourceInfo source: results.getResults().values()) 
				{
					List<Unit> stmts = new ArrayList<Unit>();
					boolean isIccResult = false;
											
					if (source.getPath() != null && source.getPath().length > 0)
					{
						for (Unit stmt : source.getPath())
						{
							stmts.add(stmt);
							if (cfg.getMethodOf(stmt).getSignature().contains("IpcSC"))
							{
								isIccResult = true;
							}
						}

					}
					
					if (isIccResult)
					{
						iccStmtsList.add(stmts);
					}
					else
					{
						stmtsList.add(stmts);
					}
				}
				
				
				
				List<String> iccResults = new ArrayList<String>();
				//for (List<Unit> stmts : iccStmtsList)
				for (int i = 0; i < iccStmtsList.size(); i++)
				{
					if (matchExtras(iccStmtsList.get(i)))
					{
						iccResults.add(iccStmtsList.get(i).toString());
					}
				}
				
				System.out.println("IccTA detects " + iccResults.size() + " ICC leaks");
				for (String ir : iccResults)
				{
					System.out.println(ir);
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public static final String intentCls = "android.content.Intent";
	public static final String bundleCls = "android.os.Bundle";
	
	public static boolean matchExtras(List<Unit> units)
	{
		List<String> seqs = new ArrayList<String>();
		for (Unit unit : units)
		{
			Stmt stmt = (Stmt) unit;
			
			if (! stmt.containsInvokeExpr())
			{
				continue;
			}
			
			SootMethod sm = stmt.getInvokeExpr().getMethod();
			String methodName = sm.getName();

			if ( (sm.getDeclaringClass().toString().equals(intentCls) && methodName.contains("Extra")) ||
				 (sm.getDeclaringClass().toString().equals(bundleCls)))
			{
				String extraKey = "";
				if (stmt.getInvokeExpr().getArgs().size() > 0)
				{
					Value v = stmt.getInvokeExpr().getArgs().get(0);
					if (v.toString().contains("\""))
					{
						extraKey = v.toString();
					}
					else
					{
						extraKey = "<anything>";
					}
				}
				
				if (methodName.startsWith("get"))
				{
					seqs.add("GET:" + extraKey);
				}
				else
				{
					seqs.add("PUT:" + extraKey);
				}
			}
		}
		
		if (seqs.contains("GET:<anything>") || seqs.contains("PUT:<anything>"))
		{
			return true;
		}
		
		while (0 < seqs.size())
		{
			String extraKey = seqs.get(0);
			seqs.remove(0);
			if (extraKey.startsWith("GET:"))
			{
				continue;
			}
			else
			{
				extraKey = extraKey.replace("PUT:", "GET:");
				if (seqs.contains(extraKey))
				{
					return true;
				}
			}
		}
		
		return false;
	}
}
