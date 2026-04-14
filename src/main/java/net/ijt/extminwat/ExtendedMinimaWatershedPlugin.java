/**
 * 
 */
package net.ijt.extminwat;

import java.awt.Color;
import java.awt.image.ColorModel;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.color.ColorMaps;
import inra.ijpb.color.ColorMaps.CommonLabelMaps;
import inra.ijpb.data.image.Images3D;
import inra.ijpb.morphology.MinimaAndMaxima;
import inra.ijpb.morphology.MinimaAndMaxima3D;
import inra.ijpb.util.IJUtils;
import inra.ijpb.watershed.Watershed;

/**
 * 
 */
public class ExtendedMinimaWatershedPlugin implements PlugIn
{
    private static String[] conn2dLabels = new String[] { "4", "8" };
    private static int[] conn2dValues = new int[] { 4, 8 };
    private static String[] conn3dLabels = new String[] { "6", "26" };
    private static int[] conn3dValues = new int[] { 6, 26 };

    @Override
    public void run(String arg)
    {
        // retrieve current image
        ImagePlus inputImagePlus = IJ.getImage();
        ImageStack image = inputImagePlus.getImageStack();

        boolean is2D = inputImagePlus.getStackSize() == 1;

        // create the dialog, with operator options
        GenericDialog gd = new GenericDialog("Extended Minima Watershed");
        int nDigits = inputImagePlus.getBitDepth() == 32 ? 2 : 0;
        gd.addNumericField("Tolerance:", 10, nDigits);
        String[] connLabels = is2D ? conn2dLabels : conn3dLabels;
        gd.addChoice("Connectivity:", connLabels, connLabels[0]);
        gd.addCheckbox("Verbose", true);
        boolean calculateDams = true;

        // wait for user input
        gd.showDialog();
        // If cancel was clicked, do nothing
        if (gd.wasCanceled()) return;

        // parse options
        double dynamic = gd.getNextNumber();
        int connIndex = gd.getNextChoiceIndex();
        int connectivity = is2D ? conn2dValues[connIndex] : conn3dValues[connIndex];
        boolean verbose = gd.getNextBoolean();
        
        final long t0 = System.currentTimeMillis();

        if (verbose) IJ.log("Running extended minima with dynamic value " + dynamic + "...");
        ImageStack minima;
        if (is2D)
        {
            minima = createStack(MinimaAndMaxima.extendedMinima(image.getProcessor(1), dynamic, connectivity));
        }
        else
        {
            // 3D processing
            minima = MinimaAndMaxima3D.extendedMinima(image, dynamic, connectivity);
        }

        final long step0 = System.currentTimeMillis();

        if (null == minima)
        {
            showBreakupMessage("The segmentation was interrupted!");
            return;
        }

        final long step1 = System.currentTimeMillis();
        if (verbose) IJ.log("Regional minima took " + (step1 - step0) + " ms.");

        // Impose extended minima over the original image
        if (verbose) IJ.log("Imposing regional minima on original image (connectivity = " + connectivity + ")...");
        ImageStack imposedMinimaStack;
        if (is2D)
        {
            imposedMinimaStack = createStack(MinimaAndMaxima.imposeMinima(image.getProcessor(1), minima.getProcessor(1), connectivity));
        }
        else
        {
            imposedMinimaStack = MinimaAndMaxima3D.imposeMinima(image, minima, connectivity);
        }

        if (null == imposedMinimaStack)
        {
            showBreakupMessage("The segmentation was interrupted!");
            return;
        }

        final long step2 = System.currentTimeMillis();
        if (verbose) IJ.log("Imposition took " + (step2 - step1) + " ms.");

        // Label regional minima
        if (verbose) IJ.log("Labeling regional minima...");
        ImageStack labeledMinima;
        if (is2D)
        {
            labeledMinima = createStack(BinaryImages.componentsLabeling(minima.getProcessor(1), connectivity, 32));
        }
        else
        {
            // 3D processing
            labeledMinima = BinaryImages.componentsLabeling(minima, connectivity, 32);
        }
        if (null == labeledMinima)
        {
            showBreakupMessage("The segmentation was interrupted!");
            return;
        }

        final long step3 = System.currentTimeMillis();
        if (verbose) IJ.log("Connected components took " + (step3 - step2) + " ms.");

        // Apply watershed
        if (verbose) IJ.log("Running watershed...");
        ImageStack result = null;
        try
        {
            if (is2D)
            {
                result = createStack(Watershed.computeWatershed(imposedMinimaStack.getProcessor(1), labeledMinima.getProcessor(1), connectivity, calculateDams, verbose));
            }
            else
            {
                result = Watershed.computeWatershed(imposedMinimaStack, labeledMinima, connectivity, calculateDams, verbose);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            IJ.log("Error while runing watershed: " + ex.getMessage());
        }
        catch (OutOfMemoryError err)
        {
            err.printStackTrace();
            IJ.log("Error: the plugin run out of memory. Please use a smaller input image.");
        }
        if (null == result)
        {
            showBreakupMessage("The segmentation was interrupted!");
            return;
        }

        String newName = createResultImageName(inputImagePlus);
        ImagePlus resultImage = new ImagePlus(newName, result);
        resultImage.setCalibration(inputImagePlus.getCalibration());

        final long end = System.currentTimeMillis();
        if (verbose) IJ.log("Whole plugin took " + (end - t0) + " ms.");

        // Adjust min and max values to display
        Images3D.optimizeDisplayRange(resultImage);

        byte[][] colorMap = CommonLabelMaps.GLASBEY_BRIGHT.computeLut(calculateDams ? 255 : 256, false);
        ColorModel cm = calculateDams 
                ? ColorMaps.createColorModel(colorMap, Color.BLACK) 
                : ColorMaps.createColorModel(colorMap);
        resultImage.getProcessor().setColorModel(cm);
        resultImage.getImageStack().setColorModel(cm);

        resultImage.show();
        IJUtils.showElapsedTime("Watershed", end - t0, inputImagePlus);
    }

    private ImageStack createStack(ImageProcessor slice)
    {
        ImageStack stack = new ImageStack(slice.getWidth(), slice.getHeight());
        stack.addSlice(slice);
        return stack;
    }

    private void showBreakupMessage(String message)
    {
        IJ.log(message);
        IJ.showStatus(message);
        IJ.showProgress(1.0);
    }
    
    private static String createResultImageName(ImagePlus baseImage)
    {
        return baseImage.getShortTitle() + "-watershed";
    }
}
