package org.graphwalker.views

import javafx.animation.Interpolator
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction

class BestFitSplineInterpolator internal constructor(x: DoubleArray, y: DoubleArray) : Interpolator() {

    private val f: PolynomialSplineFunction = SplineInterpolator().interpolate(x, y)

    override fun curve(t: Double): Double {
        return f.value(t)
    }
}