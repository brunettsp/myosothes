
// files of the myofibers' rois "_cp_outlines.txt" must be in the following folder :
// Base Directory containing the "_cp_outlines.txt" files - change manually

import static qupath.lib.gui.scripting.QPEx.*
import qupath.lib.objects.PathObjects
import qupath.lib.regions.ImagePlane



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



