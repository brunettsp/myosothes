
import static qupath.lib.gui.scripting.QPEx.*

className = args[0]

def prediction = getPathClass(className)

selected = getAnnotationObjects().findAll{it.getPathClass()==null}

for (def annotation in selected){
    annotation.setPathClass(prediction)
}

