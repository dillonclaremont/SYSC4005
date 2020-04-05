package globals;

public enum Lambda {
    SERVINSP1(0.09654457318),
    SERVINSP22(0.06436288999),
    SERVINSP23(0.04846662112),
    WORKSTATION1(0.2171827774),
    WORKSTATION2(0.09015013604),
    WORKSTATION3(0.1136934688);

    public final Double value;

    Lambda(Double value){
        this.value = value;
    }
}
