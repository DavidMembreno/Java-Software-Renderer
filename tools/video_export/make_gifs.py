import cv2
from pathlib import Path
from PIL import Image


def avi_to_gif(avi_path, gif_path, width=480, frame_step=2):
    cap = cv2.VideoCapture(str(avi_path))
    source_fps = cap.get(cv2.CAP_PROP_FPS) or 24
    frames = []
    i = 0

    while True:
        ok, frame = cap.read()
        if not ok:
            break
        if i % frame_step == 0:
            frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)   # OpenCV uses BGR, GIFs need RGB
            h, w = frame.shape[:2]
            new_h = int(h * width / w)
            frame = cv2.resize(frame, (width, new_h), interpolation=cv2.INTER_AREA)
            frames.append(Image.fromarray(frame))
        i += 1

    cap.release()

    if not frames:
        print(f"No frames read from {avi_path}")
        return

    # Keep playback speed the same even though frames were skipped
    duration_ms = int(1000 / (source_fps / frame_step))
    frames[0].save(
        gif_path,
        save_all=True,
        append_images=frames[1:],
        duration=duration_ms,
        loop=0,          # loop forever
        optimize=True,
    )
    size_mb = gif_path.stat().st_size / (1024 * 1024)
    print(f"{gif_path.name}: {len(frames)} frames, {size_mb:.1f} MB")


if __name__ == "__main__":
    script_dir = Path(__file__).resolve().parent
    output_dir = script_dir / "output"
    media_dir = script_dir.parents[1] / "docs" / "media"
    media_dir.mkdir(parents=True, exist_ok=True)

    for avi in sorted(output_dir.glob("*.avi")):
        avi_to_gif(avi, media_dir / f"{avi.stem}.gif")