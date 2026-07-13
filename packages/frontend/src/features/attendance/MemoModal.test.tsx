import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { MemoModal } from "./MemoModal";

describe("MemoModal", () => {
  it("閲覧モードでメモ内容を表示する", () => {
    render(
      <MemoModal
        open={true}
        onOpenChange={vi.fn()}
        memo="在宅勤務"
        editable={false}
      />,
    );

    expect(screen.getByText("在宅勤務")).toBeInTheDocument();
    expect(screen.getByText("備考")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "保存" })).not.toBeInTheDocument();
  });

  it("閲覧モードでメモが空の場合はプレースホルダーを表示する", () => {
    render(
      <MemoModal
        open={true}
        onOpenChange={vi.fn()}
        memo=""
        editable={false}
      />,
    );

    expect(screen.getByText("メモはありません")).toBeInTheDocument();
  });

  it("編集モードで保存ボタンを表示する", () => {
    render(
      <MemoModal
        open={true}
        onOpenChange={vi.fn()}
        memo="テスト"
        editable={true}
        onSave={vi.fn()}
      />,
    );

    expect(screen.getByRole("button", { name: "保存" })).toBeInTheDocument();
  });

  it("保存ボタン押下でonSaveが呼ばれる", async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(
      <MemoModal
        open={true}
        onOpenChange={vi.fn()}
        memo="元のメモ"
        editable={true}
        onSave={onSave}
      />,
    );

    await user.click(screen.getByRole("button", { name: "保存" }));

    expect(onSave).toHaveBeenCalledWith("元のメモ");
  });

  it("saving中は保存ボタンが無効になる", () => {
    render(
      <MemoModal
        open={true}
        onOpenChange={vi.fn()}
        memo="テスト"
        editable={true}
        onSave={vi.fn()}
        saving={true}
      />,
    );

    expect(screen.getByRole("button", { name: "保存中..." })).toBeDisabled();
  });

  it("閉じるボタンでonOpenChangeが呼ばれる", async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(
      <MemoModal
        open={true}
        onOpenChange={onOpenChange}
        memo="テスト"
        editable={false}
      />,
    );

    await user.click(screen.getByRole("button", { name: "閉じる" }));

    expect(onOpenChange).toHaveBeenCalledWith(false);
  });
});
