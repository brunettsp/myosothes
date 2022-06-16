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

def directoryPath = '/path/to/directory/containing/masks/'
println(directoryPath)

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
double downsample = 1 // TO CHANGE (if needed)
ImagePlane plane = ImagePlane.getDefaultPlane()
def imageData = getCurrentImageData()

def server = imageData.getServer()

height = server.getHeight()
width = server.getWidth()


def tile_size = 2000

lines = []

def i_x = 0
def i_y = 0


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


