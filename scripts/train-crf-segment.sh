#! /bin/bash

java -Xmx8192m -cp lib-segment-apps/target/lib-segment-apps-1.0-rc1-jar-with-dependencies.jar me.yingrui.segment.crf.app.CRFSegmentTrainingApp $@

java -Xmx8192m -cp lib-segment-apps/target/lib-segment-apps-1.0-rc1-jar-with-dependencies.jar me.yingrui.segment.crf.app.CRFSegmentTestApp $@

