package me.wowkfccc.orchestratorVelocity;


import me.wowkfccc.Ports;

public class VelocityScaler implements Ports.ScalerOps {
    private final VelocityConfig C;
    private int running = 1;

    public VelocityScaler(VelocityConfig C) { this.C = C; }

    @Override
    public int runningCount() { return running; }

    @Override
    public void requestScaleTo(int n) {
        this.running = n;
        if (C.logPlanOnly) {
            System.out.println("[MLP-Velocity] scale request -> " + n + " (log only)");
        }
        // 若要真擴縮，可加 REST call 或雲 API
    }
}

