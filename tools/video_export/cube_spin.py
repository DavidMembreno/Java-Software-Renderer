import cv2
import os
from pathlib import Path


def create_video(input_folder, output_video_file, frame_rate=24):
    images = []

    # Collect frames with filename format: orbiting_cube_1.png ... orbiting_cube_150.png
    for i in range(1, 151):
        image_path = os.path.join(input_folder, f"orbiting_cube_{i}.png")
        if os.path.exists(image_path):
            images.append(image_path)
        else:
            print(f"Stopped at missing frame: {image_path}")
            break

    if not images:
        print("No frames found!")
        return

    # Load first frame to get video dimensions
    first_frame = cv2.imread(images[0])
    height, width, _ = first_frame.shape

    fourcc = cv2.VideoWriter_fourcc(*"DIVX")
    video_writer = cv2.VideoWriter(output_video_file, fourcc, frame_rate, (width, height))

    for image_path in images:
        frame = cv2.imread(image_path)
        video_writer.write(frame)

    video_writer.release()
    print(f"Video saved to {output_video_file} ({len(images)} frames)")


if __name__ == "__main__":
    script_dir = Path(__file__).resolve().parent      # .../tools/video_export
    output_dir = script_dir / "output"                 # .../tools/video_export/output
    output_dir.mkdir(exist_ok=True)

    input_folder = r"C:\Users\DM77\Documents\Java-Software-Renderer"
    output_video_file = output_dir / "cube_orbit.avi"

    create_video(str(input_folder), str(output_video_file), frame_rate=15)