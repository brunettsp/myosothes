import static qupath.lib.gui.scripting.QPEx.*
import qupath.lib.common.GeneralTools

path = '/path/to/ground/truth/geojson/file/'

File folder = new File(path);
File[] listOfFiles = folder.listFiles();


def imageName = getProjectEntry().getImageName()

currentImport = listOfFiles.find{GeneralTools.getNameWithoutExtension(it.getPath()).contains(GeneralTools.getNameWithoutExtension(getProjectEntry().getImageName())) &&  it.toString().contains(".zip")}

importObjectsFromFile(currentImport.toString())

println("Import done")
