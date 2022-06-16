from PIL import Image
import sys, os

def transform_masks_to_black(PILimage, width, height, masks):
    pixels = PILimage.load()
    for i in range(width) :#every pixel:
        for j in range(height):
            if (pixels[i,j] in masks) :
                # change to black if in masks
                pixels[i,j] = 0  

def remove_stripe_masks(PILimage, axis, width, height, overlap_size):
    valid_axis = {'horizontal', 'vertical'}
    if axis not in valid_axis:
        raise ValueError("Wrong axis given: axis can be 'horizontal' or 'vertical'")
    masks_to_remove = set();
    masks_on_limit = set()
    if (axis == 'horizontal'):
        for l in range(width):
            if (PILimage.getpixel((l, min(overlap_size-1, height-1))) != 0):
                masks_on_limit.add(PILimage.getpixel((l, min(overlap_size-1, height-1))))
        for i in range(width) :#every pixel:
            for j in range(min(overlap_size, height)):
                if PILimage.getpixel((i, j)) not in masks_on_limit and PILimage.getpixel((i, j)) != 0:
                    masks_to_remove.add(PILimage.getpixel((i, j)))
        
        
    else:
        for l in range(height):
            if (PILimage.getpixel((min(overlap_size-1, width-1), l)) != 0):
                masks_on_limit.add(PILimage.getpixel((min(overlap_size-1, width-1), l)))
        
        for i in range(min(overlap_size, width)) :#every pixel:
            for j in range(height):
                if PILimage.getpixel((i, j)) not in masks_on_limit and PILimage.getpixel((i, j)) != 0 :
                    masks_to_remove.add(PILimage.getpixel((i, j)))
    
    transform_masks_to_black(PILimage, width, height, masks_to_remove)
    return




def remove_cross_border_masks(PILimage, axis, width, height):
    valid_axis = {'horizontal', 'vertical'}
    if axis not in valid_axis:
        raise ValueError("Wrong axis given: axis can be 'horizontal' or 'vertical'")
    masks_to_remove = set();
    for i in range(width):
        if (PILimage.getpixel((i, height-1))!= 0):
            masks_to_remove.add(PILimage.getpixel((i, height-1)))

    for j in range(height):
        if (PILimage.getpixel((width-1, j))!= 0):
            masks_to_remove.add(PILimage.getpixel((width-1, j)))

    transform_masks_to_black(PILimage, width, height, masks_to_remove)  
    return


if __name__ == '__main__':

    if len(sys.argv) != 4 :
        print(f"Usage: {sys.argv[0]} in_path out_path use_gpu model segm_channel diameter")
        exit()
    
    in_directory = sys.argv[1]
    out_directory = sys.argv[2]
    overlap_size = int(sys.argv[3])
    for tile in os.listdir(in_directory):
        if tile.endswith('_masks.png'):
            PILimage = Image.open(in_directory + tile)
            width, height = PILimage.size
            remove_stripe_masks(PILimage, 'horizontal', width, height, overlap_size)
            remove_stripe_masks(PILimage, 'vertical', width, height, overlap_size)
            remove_cross_border_masks(PILimage, 'horizontal', width, height)
            remove_cross_border_masks(PILimage, 'vertical', width, height)
            PILimage.save(out_directory + "/" + tile)
