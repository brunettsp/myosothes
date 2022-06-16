
import static qupath.lib.gui.scripting.QPEx.*

className = 'Prediction'

def prediction = getPathClass(className)

selected = getAnnotationObjects().findAll{it.getPathClass()==null}

for (def annotation in selected){
    annotation.setPathClass(prediction)
}

