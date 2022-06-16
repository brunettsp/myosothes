import org.locationtech.jts.geom.Geometry
import qupath.lib.common.GeneralTools
import qupath.lib.objects.PathObject
import qupath.lib.objects.PathObjects
import qupath.lib.roi.GeometryTools
import qupath.lib.roi.ROIs
import qupath.lib.gui.measure.ObservableMeasurementTableData
import static qupath.lib.gui.scripting.QPEx.*
import qupath.lib.objects.classes.PathClassFactory

th = 0.51

ob = new ObservableMeasurementTableData();
annotations = getAnnotationObjects()
println("annotations = "+annotations.size())

ob.setImageData(getCurrentImageData(),  annotations);
annotations.each {
    roi = it.getROI()
    //geom = roi.getGeometry()
    //print(geom)
    bx = roi.getBoundsX()
    by = roi.getBoundsY()
    bh = roi.getBoundsHeight()
    bw = roi.getBoundsWidth()
    it.getMeasurementList().putMeasurement("bx", bx)
    it.getMeasurementList().putMeasurement("by", by)
    it.getMeasurementList().putMeasurement("bw", bh)
    it.getMeasurementList().putMeasurement("bh", bw)

    it.getMeasurementList().putMeasurement("iou", 0)
    it.getMeasurementList().putMeasurement("Threshold", th)
    it.getMeasurementList().putMeasurement("FN", 0)
    it.getMeasurementList().putMeasurement("FP", 0)
    it.getMeasurementList().putMeasurement("TP", 0)
    it.getMeasurementList().putMeasurement("Gt", 0)
    it.getMeasurementList().putMeasurement("Pr", 0)
}


defClassGT = PathClassFactory.getPathClass("GroundTruth")
truth = annotations.findAll{it.getPathClass()==defClassGT}
println("truth: " + (truth.size()-1))

defClassPr = PathClassFactory.getPathClass("Prediction")
predictions = annotations.findAll{it.getPathClass()==defClassPr}
println("predictions: " + (predictions.size()-1))

defClassFP = PathClassFactory.getPathClass("FalsePositive")
defClassTP = PathClassFactory.getPathClass("TruePositive")
defClassFN = PathClassFactory.getPathClass("FalseNegative")



predictions.each{
    it.getMeasurementList().putMeasurement("Pr", 1)
    it.getMeasurementList().putMeasurement("FP", 1)
    it.setPathClass(defClassFP)
    
    
    bx = it.getMeasurementList().getMeasurementValue("bx")
    by = it.getMeasurementList().getMeasurementValue("by")
    bw = it.getMeasurementList().getMeasurementValue("bw")
    bh = it.getMeasurementList().getMeasurementValue("bh")

    annots = getAnnotsWithinBB(truth, bx, by, bw, bh)
    nannots = (annots.size())
    if (nannots>0){
        for (i in 0..nannots-1){
            iou = calculate_iou(it, annots[i])
            oldiou=ob.getNumericValue(it, "iou")
            if (iou>oldiou) it.getMeasurementList().putMeasurement("iou", iou)
            if (iou>th) {
                it.getMeasurementList().putMeasurement("TP", 1)
                it.getMeasurementList().putMeasurement("FP", 0)  
                it.setPathClass(defClassTP)       
            }
        }
    }
}

truth.each{
    it.getMeasurementList().putMeasurement("Gt", 1)
    it.getMeasurementList().putMeasurement("FN", 1)
    it.setPathClass(defClassFN)
    bx = it.getMeasurementList().getMeasurementValue("bx")
    by = it.getMeasurementList().getMeasurementValue("by")
    bw = it.getMeasurementList().getMeasurementValue("bw")
    bh = it.getMeasurementList().getMeasurementValue("bh")

    annots = getAnnotsWithinBB(predictions, bx, by, bw, bh)
    nannots = (annots.size())
    if (nannots>0){
        for (i in 0..nannots-1){
            iou = calculate_iou(annots[i],it)
            oldiou=ob.getNumericValue(it, "iou")
            if (iou>oldiou) it.getMeasurementList().putMeasurement("iou", iou)        
            if (iou>th) {
                //print(iou)
                it.getMeasurementList().putMeasurement("iou", iou)
                it.getMeasurementList().putMeasurement("FN", 0)
                it.setPathClass(defClassTP)
            } 
        }
    }
}

TP=0
FN=0
FP=0
annotations.each {
    TP = TP + it.getMeasurementList().getMeasurementValue("TP")
    FN = FN + it.getMeasurementList().getMeasurementValue("FN")
    FP = FP + it.getMeasurementList().getMeasurementValue("FP")
}

println("False Positive: "+FP)
println("False Negative: "+FN)
println("True Positive: "+TP)

Precision = TP / (TP + FP)
println("Precision: "+Precision)
Recall = TP / (TP + FN)
println("Recall: "+Recall)
    
if (Precision + Recall > 0){
    F1 = 2*(Precision*Recall)/(Precision+Recall)
    println("F1 score: " + F1)
} else {
    println("F1 undefined")
}

def calculate_iou(ann01, ann02){
    ann01_roi = ann01.getROI()
    ann02_roi = ann02.getROI()
    ann01_geo = ann01.getROI().getGeometry()
    ann02_geo = ann02.getROI().getGeometry()
    plane = ann01_roi.getImagePlane()

    intersect_geo = ann01_geo.intersection(ann02_geo)
    intersect_roi = GeometryTools.geometryToROI(intersect_geo, plane)
    intersect_annotation = PathObjects.createAnnotationObject(intersect_roi)
    intersect_area = intersect_annotation.getROI().getArea()

    union_geo = ann01_geo.union(ann02_geo)
    union_roi = GeometryTools.geometryToROI(union_geo, plane)
    union_annotation = PathObjects.createAnnotationObject(union_roi)
    union_area = union_annotation.getROI().getArea()
    
    iou = intersect_area / union_area
    return(iou)
}

def getAnnotsWithinBB(annotations, bx1, by1, bw1, bh1){
    x1 = bx1
    y1 = by1
    x2 = bx1+bw1
    y2 = by1+bh1
    annots = []
    annotations.each{
        x3 = it.getMeasurementList().getMeasurementValue("bx")
        y3 = it.getMeasurementList().getMeasurementValue("by")
        x4 = x3 + it.getMeasurementList().getMeasurementValue("bw")
        y4 = y3 + it.getMeasurementList().getMeasurementValue("bh")
        if (!((x1 > x4)|(x2 < x3)|(y1 > y4)|(y2 < y3))) {
            annots << it          
        }
    }
    return(annots)
}


