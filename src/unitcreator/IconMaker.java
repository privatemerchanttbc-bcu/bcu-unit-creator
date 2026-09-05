package unitcreator;

import common.system.P;
import common.system.fake.FakeGraphics;
import common.system.fake.FakeImage;
import common.system.fake.ImageBuilder;
import common.system.VImg;
import common.util.anim.AnimU;
import common.util.anim.AnimCE;
import common.util.anim.EAnimU;
import unitcreator.geom.MeasuringGraphics;

final class IconMaker {

    private static final int SIZE = 128;

    private IconMaker() {}

    static void makeIcon(AnimCE anim) {
        try {
            if (ImageBuilder.builder == null) return;
            EAnimU e = anim.getEAnim(AnimU.UType.IDLE);
            if (e == null) return;

            e.update(false);
            MeasuringGraphics mg = new MeasuringGraphics();
            e.draw(mg, new P(0f, 0f), 1f);
            float siz = 0.5f;
            P origin = new P(SIZE / 2f, SIZE * 0.9f);
            if (mg.hasBox()) {
                float bw = Math.max(1f, mg.maxX() - mg.minX());
                float bh = Math.max(1f, mg.maxY() - mg.minY());
                float s = Math.min((SIZE * 0.86f) / bw, (SIZE * 0.86f) / bh);
                siz = Math.max(0.08f, Math.min(6f, s));
                float ox = SIZE / 2f - siz * (mg.minX() + mg.maxX()) / 2f;
                float oy = SIZE / 2f - siz * (mg.minY() + mg.maxY()) / 2f;
                origin = new P(ox, oy);
            }

            FakeImage canvas = ImageBuilder.builder.build(SIZE, SIZE);
            FakeGraphics fg = new SafeFG(canvas.getGraphics());
            e.setTime(0f);
            e.draw(fg, origin, siz);

            VImg icon = new VImg(canvas);
            anim.setUni(icon);
            anim.setEdi(icon);
            Logger.log("UnitCreator: icon rendered from new animation");
        } catch (Throwable t) {
            Logger.err("UnitCreator: icon render failed", t);
        }
    }
}
