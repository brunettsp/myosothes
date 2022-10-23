import qupath.lib.objects.PathAnnotationObject
import qupath.lib.objects.classes.PathClassFactory

import groovy.io.*


import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane


import ij.gui.Wand
import qupath.lib.objects.PathObjects
import ij.IJ
import ij.process.ColorProcessor
import qupath.imagej.processing.RoiLabeling
import qupath.imagej.tools.IJTools
import java.util.regex.Matcher
import java.util.regex.Pattern

import qupath.lib.roi.GeometryTools;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;

import qupath.lib.common.GeneralTools
import static qupath.lib.gui.scripting.QPEx.*


/**
   Inspired from:
 * Create a region annotation with a fixed size in QuPath, based on the current viewer location.
 *
 * @author Pete Bankhead
 */


//inspired from https://forum.image.sc/t/qupath-transferring-objects-from-one-image-to-the-other/48122/2

def directoryPath = args[0]

File folder = new File(directoryPath);
File[] listOfFiles = folder.listFiles();

currentImport = listOfFiles.findAll{ qupath.lib.common.GeneralTools.getNameWithoutExtension(it.getPath()).contains(GeneralTools.getNameWithoutExtension(getProjectEntry().getImageName())) &&  it.toString().contains("_masks.png")}

Pattern pattern = Pattern.compile("x=\\d{1,5},y=\\d{1,5}");

xpositions = []
ypositions = []

for (File elt: currentImport) {
    Matcher matcher = pattern.matcher(elt.getName());
    matcher.find()
    result = matcher.group()
    pos = result.split("\\,")
    xpos = pos[0].split("\\=")[1]
    ypos = pos[1].split("\\=")[1]
    xpositions.add(xpos)
    ypositions.add(ypos)
}


clearAllObjects()

x_int = []
y_int = []


for (int i=0; i< xpositions.size;i++) { 
    x_int.add( xpositions[i].toInteger());
}

for (int i=0; i< ypositions.size;i++) { 
    y_int.add( ypositions[i].toInteger());
}


min_x = x_int.min() 
min_y = y_int.min()

max_x = x_int.max() 
max_y = y_int.max()


double downsample = 1 // TO CHANGE (if needed)
ImagePlane plane = ImagePlane.getDefaultPlane()
def imageData = getCurrentImageData()

def server = imageData.getServer()

height = server.getHeight()
width = server.getWidth()


def overlap = 200
def threshold = 0.85
def tile_size = 512

int index = 0



for (File file: currentImport) {
println("Masks file being imported: " + file)
    path = file.getPath()

    def imp = IJ.openImage(path)
    
    
    int n = imp.getStatistics().max as int
    if (n == 0) {
        print 'No objects found!'
        index+=1
        continue
    }
    def ip = imp.getProcessor()
        
    if (ip instanceof ColorProcessor) {
        throw new IllegalArgumentException("RGB images are not supported!")
    }
    def roisIJ = RoiLabeling.labelsToConnectedROIs(ip, n)
    
    def rois = roisIJ.collect {
        if (it == null)
                return
            //return IJTools.convertToROI(it, xpositions[index], ypositions[index], downsample, plane);
            int x = new Integer(xpositions[index])
            int y = new Integer(ypositions[index]) 
            return IJTools.convertToROI(it, -x, -y, downsample, plane);
    }
    rois = rois.findAll{null != it}
        
        // Convert QuPath ROIs to objects
        def pathObjects = rois.collect {
            return PathObjects.createDetectionObject(it)
        }
        addObjects(pathObjects)
        def detections = getDetectionObjects()
        //detections = filterDetections(detections)
        def newAnnotations = detections.collect {
            return PathObjects.createAnnotationObject(it.getROI(), it.getPathClass())
        }
        index+=1
        
        
removeObjects(detections, true)
addObjects(newAnnotations)

}


annotations = getAnnotationObjects()

annotations.each {
    roi = it.getROI()
    bx = roi.getBoundsX()
    by = roi.getBoundsY()
    bh = roi.getBoundsHeight()
    bw = roi.getBoundsWidth()
    it.getMeasurementList().putMeasurement("bx", bx)
    it.getMeasurementList().putMeasurement("by", by)
    it.getMeasurementList().putMeasurement("bw", bh)
    it.getMeasurementList().putMeasurement("bh", bw)

}

to_remove = []
for (int x=min_x; x<=max_x; x+=tile_size){
    for (int y=min_y; y<=max_y; y+=tile_size){
        def inOverlap = getAnnotationObjects().findAll {((measurement(it, 'bx') >= x && measurement(it, 'bx') <= x+overlap) 
        || (measurement(it, 'bx') +   measurement(it, 'bw') >= x && measurement(it, 'bx') +   measurement(it, 'bw') <= x+overlap))
        && ((measurement(it, 'by') +   measurement(it, 'bh') >= y && measurement(it, 'by') +   measurement(it, 'bh') <= y+overlap)
        || (measurement(it, 'by') >= y && measurement(it, 'by') <= y+overlap) )
        }        
        inOverlap.each{
            bx = it.getMeasurementList().getMeasurementValue("bx")
            by = it.getMeasurementList().getMeasurementValue("by")
            bw = it.getMeasurementList().getMeasurementValue("bw")
            bh = it.getMeasurementList().getMeasurementValue("bh")

            annots = getAnnotsWithinBB(inOverlap, bx, by, bw, bh)
            nannots = (annots.size())
            if (nannots>0){
                for (i in 0..nannots-1){
                    if (annots[i] != it){
                        ann01_roi = it.getROI()
                        ann02_roi = annots[i].getROI()
                        ann01_geo = it.getROI().getGeometry()
                        ann02_geo = annots[i].getROI().getGeometry()
                        plane = ann01_roi.getImagePlane()
                    
                        intersect_geo = ann01_geo.intersection(ann02_geo)
                        intersect_roi = GeometryTools.geometryToROI(intersect_geo, plane)
                        intersect_annotation = PathObjects.createAnnotationObject(intersect_roi)
                        intersect_area = intersect_annotation.getROI().getArea()
                    
                       
                       
                       
                        ann01_area = ann01_roi.getArea()
                        ann02_area = ann02_roi.getArea()
                        
             
                        
                        if (ann01_area > ann02_area){
                            if (intersect_area/ann02_area > threshold) {
                                to_remove.add(annots[i])
                            }
                        }
                        else {
                           if (intersect_area/ann01_area > threshold) {
                               to_remove.add(it)
                               break;
                           }
                        }
                        
                   }  
                }
            
            }

        
          } 
        
        
        
        
    }
}

removeObjects(to_remove, true)



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




