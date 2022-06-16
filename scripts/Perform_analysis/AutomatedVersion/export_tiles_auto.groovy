/**
 * Script to export image tiles.  Adapted from Quath documentation: https://qupath.readthedocs.io/en/latest/docs/advanced/exporting_images.html
 */

import groovy.io.FileType
// Get the current image (supports 'Run for project')
def imageData = getCurrentImageData()
def server = imageData.getServer()

// Define output path (here, relative to project)
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
def pathOutput = buildFilePath(PROJECT_BASE_DIR, 'tiles', name)
mkdirs(pathOutput)



classifyDetectionsByCentroid("detect_tissues")
resetSelection();
createAnnotationsFromPixelClassifier("detect_tissues", 0.0, 0.0, "INCLUDE_IGNORED")


// Define output resolution in calibrated units (e.g. µm if available)
double requestedPixelSize = 5.0

// Convert output resolution to a downsample factor
double pixelSize = imageData.getServer().getPixelCalibration().getAveragedPixelSize()
double downsample = 1

annotations = getAnnotationObjects().findAll{it.getPathClass() == getPathClass("Region*")}

print(annotations)

for (annotation: annotations){
    roi = annotation.getROI()
    print(roi)
    ImageRegion region =  ImageRegion.createInstance(roi)

    // Create an exporter that requests corresponding tiles from the original & labelled image servers
    new TileExporter(imageData)
        .region(region)
        .downsample(downsample)   // Define export resolution
        .imageExtension('.tif')   // Define file extension for original pixels (often .tif, .jpg, '.png' or '.ome.tif')
        .tileSize(argv[0])            // Define size of each tile, in pixels
        .annotatedTilesOnly(false) // If true, only export tiles if there is a (classified) annotation present
        .overlap(argv[1])              // Define overlap, in pixel units at the export resolution
        .writeTiles(pathOutput)   // Write tiles to the specified directory
    
}

def dir = new File(pathOutput)

dir.eachFile (FileType.FILES) { file ->
    String newName = file.path
    newName = newName.replaceAll(' ', "_")
file.renameTo newName
}


print 'Done!'
