/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.tensorflow.lite.examples.detection.tracking;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import org.tensorflow.lite.examples.detection.CameraActivity;
import org.tensorflow.lite.examples.detection.SpeechActivity;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Detector.Recognition;

/** A tracker that handles non-max suppression and matches existing objects to new detections. */
public class MultiBoxTracker {
  private static final float TEXT_SIZE_DIP = 18;
  private static final float MIN_SIZE = 16.0f;
  private static final int[] COLORS = {
    Color.BLUE,
    Color.RED,
    Color.GREEN,
    Color.YELLOW,
    Color.CYAN,
    Color.MAGENTA,
    Color.WHITE,
    Color.parseColor("#55FF55"),
    Color.parseColor("#FFA500"),
    Color.parseColor("#FF8888"),
    Color.parseColor("#AAAAFF"),
    Color.parseColor("#FFFFAA"),
    Color.parseColor("#55AAAA"),
    Color.parseColor("#AA33AA"),
    Color.parseColor("#0D0068")
  };
  final List<Pair<Float, RectF>> screenRects = new LinkedList<Pair<Float, RectF>>();
  private final Logger logger = new Logger();
  private final Queue<Integer> availableColors = new LinkedList<Integer>();
  private final List<TrackedRecognition> trackedObjects = new LinkedList<TrackedRecognition>();
  private final Paint boxPaint = new Paint();
  private final float textSizePx;
  private final BorderedText borderedText;
  private Matrix frameToCanvasMatrix;
  private int frameWidth;
  private int frameHeight;
  private int sensorOrientation;
  public  static String labelString ="";

  public MultiBoxTracker(final Context context) {
    for (final int color : COLORS) {
      availableColors.add(color);
    }

    boxPaint.setColor(Color.RED);
    boxPaint.setStyle(Style.STROKE);
    boxPaint.setStrokeWidth(10.0f);
    boxPaint.setStrokeCap(Cap.SQUARE);
    boxPaint.setStrokeJoin(Join.ROUND);
    boxPaint.setStrokeMiter(100);

    textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
  }

  public synchronized void setFrameConfiguration(
      final int width, final int height, final int sensorOrientation) {
    frameWidth = width;
    frameHeight = height;
    this.sensorOrientation = sensorOrientation;
  }

  public synchronized void drawDebug(final Canvas canvas) {
    final Paint textPaint = new Paint();
    textPaint.setColor(Color.WHITE);
    textPaint.setTextSize(60.0f);

    final Paint boxPaint = new Paint();
    boxPaint.setColor(Color.RED);
    boxPaint.setAlpha(200);
    boxPaint.setStyle(Style.STROKE);

    for (final Pair<Float, RectF> detection : screenRects) {
      final RectF rect = detection.second;
      canvas.drawRect(rect, boxPaint);
      canvas.drawText("" + detection.first, rect.left, rect.top, textPaint);
      borderedText.drawText(canvas, rect.centerX(), rect.centerY(), "" + detection.first);
    }
  }

  public synchronized void trackResults(final List<Recognition> results, final long timestamp) {
    logger.i("Processing %d results from %d", results.size(), timestamp);
    processResults(results);
  }

  private Matrix getFrameToCanvasMatrix() {
    return frameToCanvasMatrix;
  }

  @SuppressLint("DefaultLocale")
  public synchronized void draw(final Canvas canvas) {
    final boolean rotated = sensorOrientation % 180 == 90;
    final float multiplier =
        Math.min(
            canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
            canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));
    frameToCanvasMatrix =
        ImageUtils.getTransformationMatrix(
            frameWidth,
            frameHeight,
            (int) (multiplier * (rotated ? frameHeight : frameWidth)),
            (int) (multiplier * (rotated ? frameWidth : frameHeight)),
            sensorOrientation,
            false);
    for (final TrackedRecognition recognition : trackedObjects) {
      final RectF trackedPos = new RectF(recognition.location);

      getFrameToCanvasMatrix().mapRect(trackedPos);
      boxPaint.setColor(recognition.color);

      float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 8.0f;
      canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);

      //code for settings text on off btn

      String str_label="";
      float confe = 0;
      if(!CameraActivity.label){
        confe = (100 * recognition.detectionConfidence);
      }
      if(!CameraActivity.confedence){
        str_label = recognition.title;
      }

         // !TextUtils.isEmpty(recognition.title)
                  if(CameraActivity.label && !CameraActivity.confedence){
                    labelString = str_label;
                  }
                 if(CameraActivity.confedence && !CameraActivity.label ){
                   labelString = String.format("%.2f", confe);
                 }
                if(CameraActivity.confedence && CameraActivity.label ){
                  labelString = String.format("%s %.2f", str_label, confe);
                }

            //  ? String.format("%s %.2f", str_label, confe)
             // : String.format("%.2f", confe);


      //            borderedText.drawText(canvas, trackedPos.left + cornerSize, trackedPos.top,
      // labelString);
if(CameraActivity.queue.containsKey(labelString)) {
  int val = CameraActivity.queue.get(labelString);

  CameraActivity.queue.put(labelString, ++val);
  if (CameraActivity.queue.get(labelString) > 70) {
    Log.d("Found",labelString);
try {
  switch (labelString) {
    case "person":
      CameraActivity.soundPool.play(CameraActivity.sound1[0], 1, 1, 0, 0, 1);
      break;
    case "bicycle":
      CameraActivity.soundPool.play(CameraActivity.sound1[1], 1, 1, 0, 0, 1);
      break;

    case "car":
      CameraActivity.soundPool.play(CameraActivity.sound1[2], 1, 1, 0, 0, 1);
      break;
    case   "motorcycle":
            CameraActivity.soundPool.play(CameraActivity.sound1[3], 1, 1, 0, 0, 1);
    break;

    case "airplane":
    CameraActivity.soundPool.play(CameraActivity.sound1[4], 1, 1, 0, 0, 1);
    break;
    case "bus":
      CameraActivity.soundPool.play(CameraActivity.sound1[5], 1, 1, 0, 0, 1);
      break;
    case "train":
      CameraActivity.soundPool.play(CameraActivity.sound1[6], 1, 1, 0, 0, 1);
      break;
    case "truck":
      CameraActivity.soundPool.play(CameraActivity.sound1[7], 1, 1, 0, 0, 1);
      break;
    case "boat":
      CameraActivity.soundPool.play(CameraActivity.sound1[8], 1, 1, 0, 0, 1);
      break;
    case "traffic light":
      CameraActivity.soundPool.play(CameraActivity.sound1[9], 1, 1, 0, 0, 1);
      break;
    case "fire hydrant":
      CameraActivity.soundPool.play(CameraActivity.sound1[10], 1, 1, 0, 0, 1);
      break;
    case "stop sign":
      CameraActivity.soundPool.play(CameraActivity.sound1[11], 1, 1, 0, 0, 1);
      break;
    case "parking meter":
      CameraActivity.soundPool.play(CameraActivity.sound1[12], 1, 1, 0, 0, 1);
      break;
    case "bench":
      CameraActivity.soundPool.play(CameraActivity.sound1[13], 1, 1, 0, 0, 1);
      break;
    case  "bird":CameraActivity.soundPool.play(CameraActivity.sound1[14], 1, 1, 0, 0, 1);
      break;
    case "cat":CameraActivity.soundPool.play(CameraActivity.sound1[15], 1, 1, 0, 0, 1);
      break;
    case "dog":CameraActivity.soundPool.play(CameraActivity.sound1[16], 1, 1, 0, 0, 1);
      break;
    case "horse":CameraActivity.soundPool.play(CameraActivity.sound1[17], 1, 1, 0, 0, 1);
      break;
    case "sheep":CameraActivity.soundPool.play(CameraActivity.sound1[18], 1, 1, 0, 0, 1);
      break;
    case "cow":CameraActivity.soundPool.play(CameraActivity.sound1[19], 1, 1, 0, 0, 1);
      break;
    case   "elephant":CameraActivity.soundPool.play(CameraActivity.sound1[20], 1, 1, 0, 0, 1);
      break;
    case "bear":CameraActivity.soundPool.play(CameraActivity.sound1[21], 1, 1, 0, 0, 1);
      break;
    case  "zebra":CameraActivity.soundPool.play(CameraActivity.sound1[22], 1, 1, 0, 0, 1);
      break;
    case "giraffe":CameraActivity.soundPool.play(CameraActivity.sound1[23], 1, 1, 0, 0, 1);
      break;

    case "backpack":CameraActivity.soundPool.play(CameraActivity.sound1[24], 1, 1, 0, 0, 1);
      break;
    case "umbrella":CameraActivity.soundPool.play(CameraActivity.sound1[25], 1, 1, 0, 0, 1);
      break;

    case  "handbag":CameraActivity.soundPool.play(CameraActivity.sound1[26], 1, 1, 0, 0, 1);
      break;
    case "tie":CameraActivity.soundPool.play(CameraActivity.sound1[27], 1, 1, 0, 0, 1);
      break;
    case   "suitcase":CameraActivity.soundPool.play(CameraActivity.sound1[28], 1, 1, 0, 0, 1);
      break;
    case "frisbee":CameraActivity.soundPool.play(CameraActivity.sound1[29], 1, 1, 0, 0, 1);
      break;
    case "skis":CameraActivity.soundPool.play(CameraActivity.sound1[30], 1, 1, 0, 0, 1);
      break;
    case "snowboard":CameraActivity.soundPool.play(CameraActivity.sound1[31], 1, 1, 0, 0, 1);
      break;
    case "sports ball":CameraActivity.soundPool.play(CameraActivity.sound1[32], 1, 1, 0, 0, 1);
      break;
    case "kite":CameraActivity.soundPool.play(CameraActivity.sound1[33], 1, 1, 0, 0, 1);
      break;
    case "baseball bat":CameraActivity.soundPool.play(CameraActivity.sound1[34], 1, 1, 0, 0, 1);
      break;
    case "baseball glove":CameraActivity.soundPool.play(CameraActivity.sound1[35], 1, 1, 0, 0, 1);
      break;
    case "skateboard":CameraActivity.soundPool.play(CameraActivity.sound1[36], 1, 1, 0, 0, 1);
      break;
    case "surfboard":CameraActivity.soundPool.play(CameraActivity.sound1[37], 1, 1, 0, 0, 1);
      break;
    case "tennis racket":CameraActivity.soundPool.play(CameraActivity.sound1[38], 1, 1, 0, 0, 1);
      break;
    case "bottle":CameraActivity.soundPool.play(CameraActivity.sound1[39], 1, 1, 0, 0, 1);
      break;

    case "wine glass":CameraActivity.soundPool.play(CameraActivity.sound1[40], 1, 1, 0, 0, 1);
      break;
    case "cup":CameraActivity.soundPool.play(CameraActivity.sound1[41], 1, 1, 0, 0, 1);
      break;
    case "fork":CameraActivity.soundPool.play(CameraActivity.sound1[42], 1, 1, 0, 0, 1);
      break;
    case "knife":CameraActivity.soundPool.play(CameraActivity.sound1[43], 1, 1, 0, 0, 1);
      break;
    case "spoon":CameraActivity.soundPool.play(CameraActivity.sound1[44], 1, 1, 0, 0, 1);
      break;
    case "bowl":CameraActivity.soundPool.play(CameraActivity.sound1[45], 1, 1, 0, 0, 1);
      break;
    case  "banana":CameraActivity.soundPool.play(CameraActivity.sound1[46], 1, 1, 0, 0, 1);
      break;
    case "apple":CameraActivity.soundPool.play(CameraActivity.sound1[47], 1, 1, 0, 0, 1);
      break;
    case "sandwich":CameraActivity.soundPool.play(CameraActivity.sound1[48], 1, 1, 0, 0, 1);
      break;
    case "orange":CameraActivity.soundPool.play(CameraActivity.sound1[49], 1, 1, 0, 0, 1);
      break;
    case "broccoli":CameraActivity.soundPool.play(CameraActivity.sound1[50], 1, 1, 0, 0, 1);
      break;
    case "carrot":CameraActivity.soundPool.play(CameraActivity.sound1[51], 1, 1, 0, 0, 1);
      break;
    case "hot dog":CameraActivity.soundPool.play(CameraActivity.sound1[52], 1, 1, 0, 0, 1);
      break;
    case "pizza":CameraActivity.soundPool.play(CameraActivity.sound1[53], 1, 1, 0, 0, 1);
      break;
    case "donut":CameraActivity.soundPool.play(CameraActivity.sound1[54], 1, 1, 0, 0, 1);
      break;
    case "cake":CameraActivity.soundPool.play(CameraActivity.sound1[55], 1, 1, 0, 0, 1);
      break;
    case "chair":CameraActivity.soundPool.play(CameraActivity.sound1[56], 1, 1, 0, 0, 1);
      break;
    case "couch":CameraActivity.soundPool.play(CameraActivity.sound1[57], 1, 1, 0, 0, 1);
      break;
    case "potted plant":CameraActivity.soundPool.play(CameraActivity.sound1[58], 1, 1, 0, 0, 1);
      break;
    case "bed":CameraActivity.soundPool.play(CameraActivity.sound1[59], 1, 1, 0, 0, 1);
      break;
    case "dining table":CameraActivity.soundPool.play(CameraActivity.sound1[60], 1, 1, 0, 0, 1);
      break;
    case "toilet":CameraActivity.soundPool.play(CameraActivity.sound1[61], 1, 1, 0, 0, 1);
      break;
    case "tv":CameraActivity.soundPool.play(CameraActivity.sound1[62], 1, 1, 0, 0, 1);
      break;
    case "laptop":CameraActivity.soundPool.play(CameraActivity.sound1[63], 1, 1, 0, 0, 1);
      break;
    case "mouse":CameraActivity.soundPool.play(CameraActivity.sound1[64], 1, 1, 0, 0, 1);
      break;
    case "remote":CameraActivity.soundPool.play(CameraActivity.sound1[65], 1, 1, 0, 0, 1);
      break;
    case "keyboard":CameraActivity.soundPool.play(CameraActivity.sound1[66], 1, 1, 0, 0, 1);
      break;
    case "cell phone":CameraActivity.soundPool.play(CameraActivity.sound1[67], 1, 1, 0, 0, 1);
      break;
    case "microwave":CameraActivity.soundPool.play(CameraActivity.sound1[68], 1, 1, 0, 0, 1);
      break;
    case "oven":CameraActivity.soundPool.play(CameraActivity.sound1[69], 1, 1, 0, 0, 1);
      break;
    case "toaster":CameraActivity.soundPool.play(CameraActivity.sound1[70], 1, 1, 0, 0, 1);
      break;
    case "sink":CameraActivity.soundPool.play(CameraActivity.sound1[71], 1, 1, 0, 0, 1);
      break;
    case "refrigerator":CameraActivity.soundPool.play(CameraActivity.sound1[72], 1, 1, 0, 0, 1);
      break;
    case "book":CameraActivity.soundPool.play(CameraActivity.sound1[73], 1, 1, 0, 0, 1);
      break;
    case "clock":CameraActivity.soundPool.play(CameraActivity.sound1[74], 1, 1, 0, 0, 1);
      break;
    case "vase":CameraActivity.soundPool.play(CameraActivity.sound1[75], 1, 1, 0, 0, 1);
      break;
    case "scissors":CameraActivity.soundPool.play(CameraActivity.sound1[76], 1, 1, 0, 0, 1);
      break;
    case "teddy bear":CameraActivity.soundPool.play(CameraActivity.sound1[77], 1, 1, 0, 0, 1);
      break;
    case "hair drier":CameraActivity.soundPool.play(CameraActivity.sound1[78], 1, 1, 0, 0, 1);
      break;
    case "toothbrush":CameraActivity.soundPool.play(CameraActivity.sound1[79], 1, 1, 0, 0, 1);
      break;

    default:

  }
}catch (NullPointerException e){
  e.printStackTrace();
}
    CameraActivity.queue.clear();
  }
}else{
  CameraActivity.queue.put(labelString, 0);
}
                borderedText.drawText(
          canvas, trackedPos.left + cornerSize, trackedPos.top, labelString + "", boxPaint);
    }
  }

  private void processResults(final List<Recognition> results) {
    final List<Pair<Float, Recognition>> rectsToTrack = new LinkedList<Pair<Float, Recognition>>();

    screenRects.clear();
    final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());

    for (final Recognition result : results) {
      if (result.getLocation() == null) {
        continue;
      }
      final RectF detectionFrameRect = new RectF(result.getLocation());

      final RectF detectionScreenRect = new RectF();
      rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);

      logger.v(
          "Result! Frame: " + result.getLocation() + " mapped to screen:" + detectionScreenRect);

      screenRects.add(new Pair<Float, RectF>(result.getConfidence(), detectionScreenRect));

      if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
        logger.w("Degenerate rectangle! " + detectionFrameRect);
        continue;
      }

      rectsToTrack.add(new Pair<Float, Recognition>(result.getConfidence(), result));
    }

    trackedObjects.clear();
    if (rectsToTrack.isEmpty()) {
      logger.v("Nothing to track, aborting.");
      return;
    }

    for (final Pair<Float, Recognition> potential : rectsToTrack) {
      final TrackedRecognition trackedRecognition = new TrackedRecognition();
      trackedRecognition.detectionConfidence = potential.first;
      trackedRecognition.location = new RectF(potential.second.getLocation());
      trackedRecognition.title = potential.second.getTitle();
      trackedRecognition.color = COLORS[trackedObjects.size()];
      trackedObjects.add(trackedRecognition);

      if (trackedObjects.size() >= COLORS.length) {
        break;
      }
    }
  }

  private static class TrackedRecognition {
    RectF location;
    float detectionConfidence;
    int color;
    String title;
  }
}
