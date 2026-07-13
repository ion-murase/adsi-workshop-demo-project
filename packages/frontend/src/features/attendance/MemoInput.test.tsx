import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { MemoInput } from "./MemoInput";

function getTextarea(container: HTMLElement): HTMLTextAreaElement {
  return container.querySelector("textarea") as HTMLTextAreaElement;
}

describe("MemoInput", () => {
  it("ラベルと文字数カウンターを表示する", () => {
    render(<MemoInput value="" onChange={vi.fn()} />);

    expect(screen.getByText("メモ（任意）")).toBeInTheDocument();
    expect(screen.getByText("0/300")).toBeInTheDocument();
  });

  it("入力値に応じて文字数カウンターが更新される", () => {
    render(<MemoInput value="在宅勤務" onChange={vi.fn()} />);

    expect(screen.getByText("4/300")).toBeInTheDocument();
  });

  it("300文字入力時にカウンターが300/300を表示する", () => {
    const value300 = "あ".repeat(300);
    render(<MemoInput value={value300} onChange={vi.fn()} />);

    expect(screen.getByText("300/300")).toBeInTheDocument();
  });

  it("入力時にonChangeが呼ばれる", () => {
    const onChange = vi.fn();
    const { container } = render(<MemoInput value="" onChange={onChange} />);

    fireEvent.change(getTextarea(container), { target: { value: "テスト入力" } });

    expect(onChange).toHaveBeenCalledWith("テスト入力");
  });

  it("300文字を超える入力は300文字に切り詰められてonChangeに渡される", () => {
    const onChange = vi.fn();
    const { container } = render(<MemoInput value="" onChange={onChange} />);

    const longValue = "あ".repeat(305);
    fireEvent.change(getTextarea(container), { target: { value: longValue } });

    expect(onChange).toHaveBeenCalledWith("あ".repeat(300));
  });

  it("disabled時にtextareaが無効化される", () => {
    const { container } = render(<MemoInput value="" onChange={vi.fn()} disabled={true} />);

    expect(getTextarea(container).disabled).toBe(true);
  });
});
