import os, sys
from cellpose import utils, io, models 

in_path = "path/to/in/image/directory"
out_path = "path/to/result/storage/location"
use_gpu = True
model = "path/to/model/model_name/" #or model name for default models
channels = [[3,0]] #0 for gray, 1 for red, 2 for green, 3 for blue
diameter = 30
flow_threshold = 0.5
resample =  True

# Loads model
model = models.CellposeModel(gpu=use_gpu, model_type=model)   

#For each tile of image to be segmented
for tile in os.listdir(in_path):
        print("Cellpose will start for image {}".format(tile))
	img = io.imread(in_path + "/" + tile)

	# Computes segmentations
	masks, flows, styles = model.eval(img, diameter=diameter, flow_threshold=flow_threshold, resample=resample, channels=channels)

	# Save results as png

	io.save_masks(img, masks, flows, tile, savedir=out_path)
