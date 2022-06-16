#! /bin/bash

if [ $# -ne "12" ]; then
    echo "usage: $0 path_to_qupath_bin path_to_conda_env_bin image_name path_to_project_dir path_to_scripts tile_size overlap_size path_to_mask_storage_directory use_gpu model_name_with_path segm_channel diameter"
    exit 1
fi

path_to_qupath_bin=$1
path_to_conda_env_bin=$2
image_name=$3
path_to_project_dir=$4
path_to_scripts=$5
tile_size=$6
overlap_size=$7
path_to_mask_storage_directory=$8
use_gpu=$9
model_name_with_path=${10}
segm_channel=${11} #0 for gray, 1 for red, 2 for green, 3 for blue
diameter=${12}

bash $path_to_qupath_bin/QuPath.sh script $path_to_scripts/export_tiles.groovy -p $path_to_project_dir/project.qpproj -i $image_name --args $tile_size --args $overlap_size --save

$path_to_conda_env_bin/python $path_to_scripts/runCellpose_fragmented_auto.py $path_to_project_dir/tiles/$(echo $image_name | cut -d'.' -f1) $path_to_mask_storage_directory $use_gpu $model_name_with_path $segm_channel $diameter

$path_to_conda_env_bin/python $path_to_scripts/remove_overlap_auto.py $path_to_mask_storage_directory $path_to_mask_storage_directory $overlap_size

bash $path_to_qupath_bin/QuPath.sh script $path_to_scripts/import_fragmented_masks_auto.groovy -p $path_to_project_dir/project.qpproj -i $image_name --args $path_to_mask_storage_directory --save

bash $path_to_qupath_bin/QuPath.sh script $path_to_scripts/filter_annotation_phenotype_measurement.groovy -p $path_to_project_dir/project.qpproj -i $image_name --save
