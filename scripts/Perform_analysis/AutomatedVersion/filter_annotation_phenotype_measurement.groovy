
// Get the name of the current image file (.czi)
def imageName = getProjectEntry().getImageName()
print(imageName)


//Remove prediction artefacts using object classifier
toChange = getAnnotationObjects()
//toChange = getDetectionObjects()
newObjects = []
toChange.each{
    roi = it.getROI()
    annotation = PathObjects.createDetectionObject(roi, it.getPathClass())
    newObjects.add(annotation)
}

// Actually add the objects
addObjects(newObjects)
//Comment this line out if you want to keep the original objects
removeObjects(toChange,true)

resolveHierarchy()

def cells = getDetectionObjects()

print(cells)
selectDetections();

addShapeMeasurements("AREA", "LENGTH", "CIRCULARITY", "SOLIDITY", "MAX_DIAMETER", "MIN_DIAMETER", "NUCLEUS_CELL_RATIO")
//runObjectClassifier("residual_removal")
resetSelection();


removal = getDetectionObjects().findAll{it.getPathClass().toString().contains("Other")}
removeObjects(removal, true)

resetDetectionClassifications()

toChange = getDetectionObjects()
print(toChange)
newObjects = []
toChange.each{
    roi = it.getROI()
    annotation = PathObjects.createAnnotationObject(roi, it.getPathClass())
    newObjects.add(annotation)
}

// Actually add the objects
addObjects(newObjects)
//Comment this line out if you want to keep the original objects
removeObjects(toChange,true)

resolveHierarchy()
print("Done!")



//Create full image annotation
createSelectAllObject(true);

// Nucleus detection
// parameters of the cell detection were tuned to ensure the detection of centronucleus.
// further tuning might be necessay
// i.e.:the threshold of 0.2 may be adjusted
runPlugin('qupath.imagej.detect.cells.WatershedCellDetection', '{"detectionImageBrightfield": "Hematoxylin OD",  "requestedPixelSizeMicrons": 0.4,  "backgroundRadiusMicrons": 8.0,  "medianRadiusMicrons": 0.0,  "sigmaMicrons": 0.4,  "minAreaMicrons": 5.0,  "maxAreaMicrons": 100.0,  "threshold": 0.3,  "maxBackground": 2.0,  "watershedPostProcess": false,  "cellExpansionMicrons": 0.0,  "includeNuclei": false,  "smoothBoundaries": false,  "makeMeasurements": true}');


//create the folder (if necessary) in the qupath projet that will contain the results
def results_path = buildFilePath(PROJECT_BASE_DIR, 'InfDiam_Centro_results')
mkdirs(results_path)
print('Results will be exported to :' + "/n"+ results_path)



//Filter annotation with pixel classifier
def annotation_objects = getAnnotationObjects()

int seuil = 70
double downsample = 1.0
def server = getCurrentServer()

import qupath.lib.gui.measure.ObservableMeasurementTableData

selectAnnotations();
addPixelClassifierMeasurements("remove_artefacts", "remove_artefacts")

def toBeRemoved = []
def observable = new ObservableMeasurementTableData();

observable.setImageData(getCurrentImageData(),  annotation_objects);

for (annotation in annotation_objects){
    measurement = observable.getNumericValue(annotation, "remove_artefacts: Cell %")
    if (measurement < seuil) {
        toBeRemoved << annotation   
  }
   
}

removeObjects(toBeRemoved, true)




// the following is directly from Pete Bankhead  written for https://forum.image.sc/:
/**
 * Remove detections that have ROIs that touch the border of any annotation ROI in QuPath v0.2.
 *
 * Note that there are some non-obvious subtleties involved depending upon how ROIs are accessed -
 * see the 'useHierarchyRule' option for more info.
 *
 * Written for https://forum.image.sc/t/remove-detected-objects-touching-annotations-border/49053
 *
 * @author Pete Bankhead
 */

import org.locationtech.jts.geom.util.LinearComponentExtracter
import qupath.lib.objects.PathDetectionObject
import qupath.lib.objects.PathObject
import qupath.lib.regions.ImageRegion

import java.util.stream.Collectors

//import static qupath.lib.gui.scripting.QPEx.*

// Define the distance in pixels from an annotation boundary
// Zero is a valid option for 'touching'
double distancePixels = 5.0

// Toggle whether to use the 'hierarchy' rule, i.e. only consider detections with centroids inside an annotation
boolean useHierarchyRule = true


// Get parent annotations
def hierarchy = getCurrentHierarchy()
def annotations = hierarchy.getAnnotationObjects()

// Loop through detections
def toRemove = new HashSet<PathObject>()
for (def annotation in annotations) {
    def roi = annotation.getROI()
    if (roi == null)
        continue // Shouldn't actually happen...
    Collection<? extends PathObject> detections
    if (useHierarchyRule)
        // Warning! This decides based upon centroids (the 'normal' hierarchy rule)
        detections = hierarchy.getObjectsForRegion(PathDetectionObject.class, ImageRegion.createInstance(roi), null)
    else
        // This uses bounding boxes (the 'normal' hierarchy rule)
        detections = hierarchy.getObjectsForROI(PathDetectionObject.class, roi)
    // We need to get separate line strings for each polygon (since otherwise we get distances of zero when inside)
    def geometry = roi.getGeometry()
    for (def line in LinearComponentExtracter.getLines(geometry)) {
        toRemove.addAll(
                detections.parallelStream()
                        .filter(d -> line.isWithinDistance(d.getROI().getGeometry(), distancePixels))
                        .collect(Collectors.toList())
        )
    }
}
println "Removing ${toRemove.size()} detections without ${distancePixels} pixels of an annotation boundary"
hierarchy.removeObjects(toRemove, true)
// end of the script from Pete


// classes to discriminate centronuclear fibers
normale = getPathClass('Normale', getColorRGB(0,200,0))
centro = getPathClass('Centro', getColorRGB(200,0,0))

// Prepare the table of annoation measurements
import qupath.lib.gui.measure.ObservableMeasurementTableData
def ob = new ObservableMeasurementTableData();

def newAnnotations = getAnnotationObjects()
 // This line creates all the measurements
ob.setImageData(getCurrentImageData(),  newAnnotations);

//print(newAnnotations())
newAnnotations.each {
    def theMeasure = ob.getNumericValue(it,"Num Detections")
    if( theMeasure == 0) {
       it.setPathClass(normale)
    } else {
      it.setPathClass(centro)
    }
}

selectAnnotations();
addShapeMeasurements("MIN_DIAMETER")

saveAnnotationMeasurements(results_path)

toChange = getAnnotationObjects()
//toChange = getDetectionObjects()
newObjects = []
toChange.each{
    roi = it.getROI()
    annotation = PathObjects.createDetectionObject(roi, it.getPathClass())
    newObjects.add(annotation)
}

// Actually add the objects
addObjects(newObjects)
//Comment this line out if you want to keep the original objects
removeObjects(toChange,true)

resolveHierarchy()
createSelectAllObject(true);

print(results_path)
resetSelection();
print('Done')


