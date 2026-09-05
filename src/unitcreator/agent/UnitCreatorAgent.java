package unitcreator.agent;

import unitcreator.Logger;
import unitcreator.transform.MainPageTransformer;

import java.lang.instrument.Instrumentation;

public final class UnitCreatorAgent {

    private UnitCreatorAgent() {}

    public static void premain(String args, Instrumentation inst) {
        start(inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        start(inst);
    }

    private static void start(Instrumentation inst) {
        try {
            inst.addTransformer(new MainPageTransformer(), false);
            Logger.log("Unit Creator agent ready");
        } catch (Throwable t) {
            Logger.err("Unit Creator agent failed to start", t);
        }
    }
}
