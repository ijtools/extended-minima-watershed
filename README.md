# Extended Minima Watershed

This plugin for ImageJ/Fiji performs watershed on a grayscale image, after imposition of extended minima over the original image. 
The processing is the same as the "Morphological Segmentation" Plugin from the [MorphoLibJ library](https://github.com/ijpb/MorphoLibJ).
Actually, it relies on the MorphoLibJ library, that is required to run the plugin.

### Installation
Simply copy the jar file into the "plugins" directory.

### Usage
* Select the plugin "Plugins -> MorphoLibJ Plus -> Extended Minima Watershed"
* This opens a dialog with "tolerance", "connectivity" options (the same as the Morphological Segmentation plugin), as well as a "verbose" option, to disply progress within the log window
* Clicking OK will compute directly the final image.

The different steps are the following ones:
1. Compute extended minima from the original image
1. compute imposition of minima on the original image
1. compute connected components labeling on the minima image
1. compute maker-based watershed on the imposed image, using labeled minima as markers.
