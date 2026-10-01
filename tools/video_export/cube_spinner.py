import cv2
import os
from pathlib import Path


def create_video(input_folder, output_video_file, frame_rate=24, loops=3):
    frames = []

    # Collect frames: colored_cube_1.png ... colored_cube_37.png
    for i in range(1, 38):
        image_path = os.path.join(input_folder, f"colored_cube_{i}.png")
        if not os.path.exists(image_path):
            print(f"Missing frame: {image_path}")
            return
        frames.append(cv2.imread(image_path))

    # Use the first frame to get video dimensions
    height, width, _ = frames[0].shape

    fourcc = cv2.VideoWriter_fourcc(*"DIVX")
    video_writer = cv2.VideoWriter(output_video_file, fourcc, frame_rate, (width, height))

    # Write the full set of frames once per loop
    for _ in range(loops):
        for frame in frames:
            video_writer.write(frame)

    video_writer.release()
    print(f"Video saved to {output_video_file} ({len(frames)} frames x {loops} loops)")


if __name__ == "__main__":
    script_dir = Path(__file__).resolve().parent      # .../tools/video_export
    output_dir = script_dir / "output"                 # .../tools/video_export/output
    output_dir.mkdir(exist_ok=True)

    input_folder = r"C:\Users\DM77\Documents\Java-Software-Renderer"
    output_video_file = output_dir / "cube_rotate.avi"

    create_video(str(input_folder), str(output_video_file), frame_rate=18, loops=3)