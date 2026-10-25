package cn.blockforge.generated.adaptivesoundscape.client.gui.hub;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Hub 的内容节：build() 注册控件（经 hub.addWidget），render() 自绘内容区，
 * 鼠标事件由 HubScreen 在内容区内优先转发给当前节。
 * 节实例由 HubScreen 持久持有，切换导航时只重建控件，节内状态（草稿/滚动/选中）自然保留。
 */
abstract class Section {
    protected HubScreen hub;

    void attach(HubScreen hub) {
        this.hub = hub;
    }

    /** 控件构建：在 HubScreen.init/切换节时调用（此前已 clearWidgets）。 */
    abstract void build();

    /** 内容区自绘（在控件渲染之前调用，控件会覆盖其上）。 */
    void render(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    /** 顶层自绘（在控件渲染之后调用，用于浮窗等需置于最上的内容）。 */
    void renderLate(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    boolean mouseDragged(double mouseX, double mouseY, int button) {
        return false;
    }

    boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    /** 每客户端 tick（经 HubScreen.tick 转发），MultiLineEditBox 等需要。 */
    void tick() {
    }
}
