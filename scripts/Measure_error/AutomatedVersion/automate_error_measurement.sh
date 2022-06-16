#! /bin/bash

if [ $# -ne "6" ]; then
    echo "usage: $0 path_to_qupath_bin image_name path_to_project_dir path_to_scripts path_to_masks path_to_ground_truth"
    exit 1
fi



path_to_qupath_bin=$1
image_name=$2
path_to_project_dir=$3
path_to_scripts=$4
path_to_masks=$5
path_to_ground_truth=$6

bash $path_to_qupath_bin/QuPath.sh script $path_to_scripts/import_fragmented_masks_auto.groovy -p $path_to_project_dir/project.qpproj -i $image_name --args $path_to_masks --save

bash $path_to_qupath_bin/QuPath.sh script $path_to_scripts/SetAnnotationClass_auto.groovy -p $path_to_project_dir/project.qpproj -i $image_name --args "Prediction" --save

bash $path_to_qupath_bin/QuPath.sh script $path_to_scripts/filter_annotations.groovy -p $path_to_project_dir/project.qpproj -i $image_name --save

bash $path_to_qupath_bin/QuPath.sh script $path_to_scripts/import_ground_truth_auto.groovy -p $path_to_project_dir/project.qpproj -i $image_name --args $path_to_ground_truth --save

bash $path_to_qupath_bin/QuPath.sh script $path_to_scripts/SetAnnotationClass_auto.groovy -p $path_to_project_dir/project.qpproj -i $image_name --args "GroundTruth" --save

bash $path_to_qupath_bin/QuPath.sh script $path_to_scripts/measure_error.groovy -p $path_to_project_dir/project.qpproj -i $image_name --save
