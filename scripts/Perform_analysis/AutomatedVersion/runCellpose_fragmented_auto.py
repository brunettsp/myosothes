import os, sys
from cellpose import utils, io, models 


if len(sys.argv) != 7 :
    print(f"Usage: {sys.argv[0]} in_path out_path use_gpu model segm_channel diameter")
    exit()

in_path = sys.argv[1]
out_path = sys.argv[2]
if (sys.argv[3].lower() == "true"):
    use_gpu = True
elif (sys.argv[3].lower() == "false"):
    use_gpu = False
else :
    print("Invalid argument for use_gpu statement.")
    exit()
    
model = sys.argv[4]
channels = [[int(sys.argv[5]),0]] #0 for gray, 1 for red, 2 for green, 3 for blue
diameter = int(sys.argv[6])
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
