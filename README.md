# Required tools and libraries installation

Two tools are required to run workflow and need to be installed:
1. Install QuPath 0.3.1 using documentation available at: https://github.com/qupath/qupath
2. Install Cellpose 2.0.4 using anaconda environment following user guide available at: https://github.com/MouseLand/cellpose
3. Install Pillow python library in previously created anaconda environment: pip install pillow==8.0.1.

# Preliminary setup

Add classifiers directory in your QuPath project directory and Cellpose_retrained_model/CP_20220420_140301 file in $HOME/.cellpose/models directory.

Add images to be analyzed to the QuPath project. 

# Perform analysis

## Manual version

To execute entire workflow manually, available scripts must be executed in following order:

1. In QuPath environment, tile opened image and export them in tif format with scripts/Perform_analysis/ManualVersion/export_tiles.groovy script. Tiles will be exported with specific naming style and stored in the project in tiles/imageNameWithoutExtension directory: imageNameWithoutExtension_[x=xValue,y=yValue,w=width,h=height].tif
2. In previously created anaconda environment, execute scripts/Perform_analysis/ManualVersion/runCellpose_fragmented.py after setting up all variables at the script top. 
3. In same environment, run scripts/Perform_analysis/ManualVersion/remove_overlap.py script after setting up required parameters in main loop.
4. In QuPath environment, execute groovy script scripts/Perform_analysis/ManualVersion/import_fragmented_masks.groovy after specify masks directory location at its beginning.
5. In QuPath environment, run analysis using scripts/Perform_analysis/ManualVersion/filter_annotation_phenotype_measurement.groovy script.

Results are stored under InfDiam_Centro_results directory available in QuPath project directory.

## Automated version

It is possible to execute the entire workflow with a single line of command in the terminal with the automated version.

1. Go to directory scripts/Perform_analysis/AutomatedVersion
2. In terminal, type the following command, replacing the arguments with their values : `./automate_analysis.sh path_to_qupath_bin path_to_conda_env_bin image_name path_to_project_dir path_to_scripts tile_size overlap_size path_to_mask_storage_directory use_gpu model_name_with_path segm_channel diameter` 

Results are stored under InfDiam_Centro_results directory available in QuPath project directory.

# Segmentation error measurement

## Manual version

The error measurement workflow used to tune our workflow can be executed in QuPath environment in following order:

1. Run groovy script scripts/Measure_error/ManualVersion/import_fragmented_masks.groovy after specify masks directory location at its beginning
2. Assign them a label by executing  them the "Prediction" using set up scripts/Measure_error/ManualVersion/SetAnnotationClass.groovy script.
3. Filter masks using scripts/Measure_error/ManualVersion/filter_annotations.groovy script.
4. Import ground truth masks stored in GeoJSON format with scripts/Measure_error/ManualVersion/import_ground_truth.groovy script with indicated directory location.
5. Assign them the "VeriteTerrain" class using scripts/Measure_error/ManualVersion/SetAnnotationClass.groovy again with updated class name.
6. Run scripts/Measure_error/ManualVersion/measure_error.groovy script to get the F1-score indicator. 

## Automated version

It is possible to execute the entire error measurement workflow with a single line of command in the terminal with the automated version. 

1. Go to directory scripts/Measure_error/AutomatedVersion
2. In terminal, type the following command, replacing the arguments with their values : `./automate_error_measurement.sh path_to_qupath_bin image_name path_to_project_dir path_to_scripts path_to_masks path_to_ground_truth` 
