package net.unit8.job_streamer.agent.util;

import clojure.lang.*;

/**
 * @author Yuki Seki
 */
public class SystemUtil {
  public static Object getSystem() {
    Object system = null;
    try{
      Object mainSystemAtom = RT.var("job-streamer.agent.main", "system").get();
      system = ((Atom) mainSystemAtom).deref();
    } catch(Throwable ignore){
      system = RT.var("reloaded.repl", "system").get();
    } finally{
      return system;
    }
  }
}

