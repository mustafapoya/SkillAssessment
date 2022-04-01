package net.golbarg.skillassessment.models;

public class PieChartData {
    String label;
    float value;
    int color;

    public PieChartData(String label, float value, int color) {
        this.label = label;
        this.value = value;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return "PieChartData{" +
                "label='" + label + '\'' +
                ", value=" + value +
                ", color=" + color +
                '}';
    }
}
