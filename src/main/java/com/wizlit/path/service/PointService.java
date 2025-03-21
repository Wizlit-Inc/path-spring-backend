package com.wizlit.path.service;

import com.wizlit.path.entity.Point;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PointService {
    Mono<Point> createPoint(Point point);

    Mono<List<Point>> getAllPoints();

    Mono<Point> addMiddlePoint(String startPoint, String endPoint, Point middlePoint, int depth);
}
