function initDvJS() {
  
  function initGeo() {
    // Coordinates mapping in GEOBOX datafield
    const X1 = 'W';
    const Y1 = 'S';
    const X2 = 'E';
    const Y2 = 'N';
    const TEXT_AREA_COORDINATES = 'textAreaCoordinates'

    const MAX_ZOOM = 19;
    var bounds = [
      [-85, -180], // Southwest corner (near Antarctica)
      [85, 180]    // Northeast corner (near the Arctic)
    ];
    const READ_ONLY_INIT_MAP_OPTS = {
      center: [52.1145028, 19.4235611],
      zoom: 4,
      minZoom: 2,
    }
    const INIT_MAP_OPTS = {
      ...READ_ONLY_INIT_MAP_OPTS,
      editable: true,
      bounds: bounds,
      maxBounds:  bounds, // Restrict view to these bounds
      maxBoundsViscosity: 1.0 // Prevents dragging outside
    };

    const TILE_LAYER_URL = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
    const TILE_LAYER_COPYRIGHT = '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>';

    // Initialize JSTS geometry factory
    var geoFactory = new jsts.geom.GeometryFactory();

    // Store value from input field
    function putValue(dataMap, key, field, value) {
      if (!isNaN(value) && value !== '') {
        let data = dataMap.get(key);
        data.values[field] = Number(value);
      }
    }

    // Mass initialization of maps (when section is shown/visible)
    function initializeAll(dataMap, keyPrefix, initializer) {
      for (const [key, data] of dataMap) {
        if (!key.startsWith(keyPrefix) || data.leafMapInitialized) {
          continue;
        }
        initializer(key, data);
        data.leafMapInitialized = true;
      }
    }

    function hasPolygonThreePoints(layer) {
      return layer.getLatLngs()[0].length >= 3;
    }

    function isSelfIntersecting(layer, currentGeo, isEditMode) {
      if (!hasPolygonThreePoints(layer)) {
        return false;
      }

      var latlngs = layer.getLatLngs()[0]; // Get polygon coordinates
      var coords = latlngs.map(function (latlng) {
          return new jsts.geom.Coordinate(latlng.lng, latlng.lat);
      });

      // add current geo only when drawing mode is on
      // used for move event to check new vertex location before creating
      if (currentGeo && isEditMode) {
        coords.push(new jsts.geom.Coordinate(currentGeo.lng, currentGeo.lat));
      }
      coords.push(coords[0]); // Close the polygon

      return isSelfIntersectingCoordinates(coords);
    }

    function isSelfIntersectingCoordinates(coordinates) {
     // Create JSTS polygon
      var linearRing = geoFactory.createLinearRing(coordinates);
      var polygon = geoFactory.createPolygon(linearRing);

      return !polygon.isSimple();
    }

    // MetadataView – methods & data for Dataset/Metadata tab

    // Draw map with geobox rectangle (must be called only when the target div is visible!)
    function initializeMapInMetadataView(key, data) {
      data.leafMap = L.map(key, READ_ONLY_INIT_MAP_OPTS);
      let leafMap = data.leafMap;
      leafMap.invalidateSize();

      data.polygonLayer = L.layerGroup().addTo(leafMap);
      updateMapCoordinates(data, data.values[TEXT_AREA_COORDINATES])
      
      L.tileLayer(TILE_LAYER_URL, { maxZoom: MAX_ZOOM, attribution: TILE_LAYER_COPYRIGHT }).addTo(leafMap);
    }

    let metadataMapsData = new Map();

    let metadataView = {
      prepare: function (key) {
        metadataMapsData.set(key, {
          leafMap: undefined,
          leafMapInitialized: false,
          values: {}
        });
      },

      putValue: function (key, field, value) {
        putValue(metadataMapsData, key, field, value);
      },
      storeCoordinates: function(key, value) {
        let data = metadataMapsData.get(key);
        data.values[TEXT_AREA_COORDINATES] = value;
      },
      initializeAll: function (keyPrefix) {
        initializeAll(metadataMapsData, keyPrefix, initializeMapInMetadataView);
      }
    }

    // EditView – methods & data for edit forms (also in search)

    let editMapsData = new Map();

    function onMarkerDragged(data) {
      if (!data || !data.markerA || !data.markerB) {
        return;
      }
      let bounds = L.latLngBounds([data.markerA.getLatLng(), data.markerB.getLatLng()]);
      data.selection.setBounds(bounds);
      centerMap(data, bounds);
      updateInputs(data);
    }
    
    function centerMap(data, bounds) {
      let extendedBounds = bounds.pad(0.1);
      data.leafMap.fitBounds(extendedBounds);
    }

    function initializeMapInView(key, data) {
      data.leafMap = L.map(key, INIT_MAP_OPTS);
      let map = data.leafMap;
      map.invalidateSize();

      data.polygonLayer = L.layerGroup().addTo(map);
      setupMapEventsHandlers(map, data);
      createBaseEditControls();
      activateDrawingTools(map, data)

      L.tileLayer(TILE_LAYER_URL, { maxZoom: MAX_ZOOM, attribution: TILE_LAYER_COPYRIGHT }).addTo(map);
      this.updateMap(key);
    }

    function setupMapEventsHandlers(map, data) {
      map.on('editable:drawing:commit', function (e) {
        createdShape(e ,data);
      });
      map.on('editable:edited', function (e) {
        updateTextArea(e.layer, data);
      });
      map.on('editable:vertex:new', function (e) {
        updateTextArea(e.layer, data);
      });
      map.on('editable:vertex:deleted', function (e) {
        updateTextArea(e.layer, data);
      });

      // only Polygon event
      map.on('editable:vertex:dragend', function (e) {
        if (e.layer instanceof L.Rectangle || e.layer instanceof L.Marker) {
          return;
        }

        if (isSelfIntersecting(e.layer, e.latlng, map.editTools.drawing())) {
          e.vertex.delete();
          e.layer.setStyle({ color: '#3388ff' });
        }
      });

      // only Polygon event
      // cancel click for polygons and preventing creating new vertex
      map.on('editable:drawing:click', function (e) {
        if (e.layer instanceof L.Rectangle || e.layer instanceof L.Marker) {
          return;
        }

        if (isSelfIntersecting(e.layer, e.latlng, map.editTools.drawing())) {
          e.cancel();
        }
      });

      // only Polygon event
      // detect if polygon is self intersecting and change color of such polygon
      map.on('editable:drawing:move', function (e) {
        if (e.layer instanceof L.Rectangle || e.layer instanceof L.Marker) {
          return;
        }

        // for three points no need to check intersection
        // also dashed lines were not rendered properly when style was changed for 2,3 points
        if (!hasPolygonThreePoints(e.layer)) {
          return;
        }

        if (isSelfIntersecting(e.layer, e.latlng, map.editTools.drawing())) {
          e.layer.setStyle({ color: 'red' });
        } else {
          e.layer.setStyle({ color: '#3388ff' });
        }
      });
    }

    function activateDrawingTools(map, data) {
      if (data.drawTools.allowMarker) {
        createNewMarkerControl(map, data.polygonLayer);
      }
      if (data.drawTools.allowPolygon) {
        createNewPolygonControl(map, data.polygonLayer);
      }
      if (data.drawTools.allowRectangle) {
        createNewRectangleControl(map, data.polygonLayer);
      }
    }

    function createBaseEditControls() {
      L.EditControl = L.Control.extend({
        options: {
          position: 'topleft',
          callback: null,
          kind: '',
          html: ''
         },
        onAdd: function (map) {
          var container = L.DomUtil.create('div', 'leaflet-control leaflet-bar'),
          link = L.DomUtil.create('a', '', container);

          link.href = '#';
          link.title = 'Create a new ' + this.options.kind;
          link.innerHTML = this.options.html;
          L.DomEvent.on(link, 'click', L.DomEvent.stop)
                    .on(link, 'click', function () {
                      window.LAYER = this.options.callback.call(map.editTools);
                    }, this);

          return container;
        }
      });
    }

    function createNewMarkerControl(map, polygonLayer) {
      L.NewMarkerControl = L.EditControl.extend({
        options: {
          position: 'topleft',
          callback: function () {
            polygonLayer.clearLayers();
            map.editTools.startMarker();
          },
          kind: 'marker',
          html: '<svg width="20" height="20" viewBox="0 0 100 100" style="margin-top:5px">' +
                  '<path d="M50,10 C70,10 80,30 50,90 C20,30 30,10 50,10 Z" fill="black" stroke="black" stroke-width="5"/>' +
                  '<circle cx="50" cy="35" r="10" fill="white" stroke="black" stroke-width="3"/>' +
                '</svg>'
        }
      });

      map.addControl(new L.NewMarkerControl());
    }

    function createNewPolygonControl(map, polygonLayer) {
      L.NewPolygonControl = L.EditControl.extend({
        options: {
          position: 'topleft',
          callback: function () {
            polygonLayer.clearLayers();
            var polygon = map.editTools.startPolygon();

          },
          kind: 'polygon',
          html: '<svg width="20" height="20" viewBox="0 0 100 100" style="margin-top:5px">' +
                  '<polygon points="50,10 90,30 90,70 50,90 10,70 10,30" fill="black" stroke="black" stroke-width="5"/>' +
                '</svg>'
        }
      });

      map.addControl(new L.NewPolygonControl());
    }

    function createNewRectangleControl(map, polygonLayer) {
      L.NewRectangleControl = L.EditControl.extend({
        options: {
          position: 'topleft',
          callback: function () {
            polygonLayer.clearLayers();
            map.editTools.startRectangle();
          },
          kind: 'rectangle',
          html: '<svg width="20" height="20" viewBox="0 0 100 100" style="margin-top:5px">' +
                  '<polygon points="10,10 90,10 90,90 10,90 10,10" fill="black" stroke="black" stroke-width="5"/>' +
                '</svg>'
        }
      });

      map.addControl(new L.NewRectangleControl());
    }

    function createdShape(e, data) {
        let layer = e.layer;
        data.polygonLayer.addLayer(layer);

        updateTextArea(layer, data);
    }

    function updateTextArea(layer, data) {
      const widgetVars = data.widgetVars;
      if (!PF(widgetVars['polygonGeo'])) {
        throw new Error('Missing dataset field supporting polygon edit: polygonGeo');
      }

      const geometry = layer.toGeoJSON().geometry.coordinates;

      const isPolygon = Array.isArray(geometry[0]);
      if (isPolygon) {
        if (data.advancedSearch) {
          result = normalizeToRectangle(geometry)
            .map(row => row.join(" "))
            .join("\n");
        } else {
          result = geometry[0].map(row => row.join(" ")).join("\n");
        }
      } else {
        result = geometry.join(" ");
      }
      PF(widgetVars['polygonGeo']).jq.val(result);
    }

    function normalizeToRectangle(coordinates) {
      const latList = coordinates[0].map(p => p[0]);
      const lonList = coordinates[0].map(p => p[1]);

      const minLat = Math.min(...latList);
      const maxLat = Math.max(...latList);
      const minLon = Math.min(...lonList);
      const maxLon = Math.max(...lonList);

      return [
       [maxLat, minLon],
       [maxLat, maxLon],
       [minLat, maxLon],
       [minLat, minLon],
       [maxLat, minLon]
      ];
    }

    function parseCoordinates(textCoordinates) {
      if (!textCoordinates) {
        return [];
      }

      const hasLetters = /[a-zA-Z]/.test(textCoordinates);
      if (hasLetters) {
        return [];
      }

      const cords = textCoordinates
        .trim()
        .split("\n")
        .map(line => line.trim().split(/\s+/).map(Number))
        .map(coord => [coord[1], coord[0]]);

        // validation
        const hasTwoNumericPoints = cords.every(cord =>
          Array.isArray(cord) &&
          cord.length === 2 &&
          cord.every(point => !isNaN(point))
        );
        const allPointsWithinBounds = cords.every(([lat, lon]) =>
          lat >= -90 && lat <= 90 &&
          lon >= -180 && lon <= 180
        );
        if (cords.length == 0 || !hasTwoNumericPoints || !allPointsWithinBounds) {
            return [];
        }

        return cords
    }

    // Draw on map: marker, line, polygon using list of coordinates
    // coordinates - excepted format, each line represent pair of longitude, latitude
    // e.q.
    // 1.112 41.12
    // 2.12 15.21
    function updateMapCoordinates(data, coordinates) {
      if (!data.polygonLayer) {
        return;
      }

      data.polygonLayer.clearLayers();
      if (!coordinates || coordinates.trim() === '') {
        return;
      }

      var cords = parseCoordinates(coordinates);
      if (cords.length == 0) {
          return;
      }

      var shape;
      if (cords.length == 1) {
          shape = L.marker(cords[0]).addTo(data.polygonLayer)
      } else if (cords.length == 2) {
          shape = L.polyline(cords).addTo(data.polygonLayer)
      } else if (cords.length >= 3) {
          if (isRectangleAxisAligned(cords)) {
            shape = L.rectangle(cords).addTo(data.polygonLayer);
          } else {
            var polygon = L.polygon(cords);
            if (isSelfIntersecting(polygon, null, false)) {
              console.log('updateMapCoordinates was not possible, polygon is self intersecting');
              return;
            }

            shape = polygon.addTo(data.polygonLayer);
          }
      }

      const bounds = L.latLngBounds(cords);
      centerMap(data, bounds);
    }

    // use html <template> with possibility to replace values from data
    // templateId - id of template
    // data - any json that will be used to replace placeholders {{placeholder}}
    function renderTemplate(templateId, data) {
        let template = document.getElementById(templateId).innerHTML;

        return template.replace(/\{\{(.*?)\}\}/g, (_, key) => {
            let keys = key.trim().split('.');
            // allow support nested placeholder {{test.test}}
            return keys.reduce((obj, k) => (obj && obj[k] !== undefined ? obj[k] : ''), data);
        });
    }

     // Checks if the given points form a rectangle or square
     // This method works only for axis-aligned rectangles (rectangles that are not rotated)
    function isRectangleAxisAligned(coordinates) {
      if (coordinates.length === 0) {
        return false;
      }

      coordinates = removeRedundantPoints(coordinates);
      if (coordinates.length !== 4) {
        return false;
      }

      // Order coordinates: [0] Top-left, [1] Top-right, [2] Bottom-left, [3] Bottom-right
      coordinates.sort((a, b) => b[0] - a[0] || a[1] - b[1]);
      const [topLeft, topRight, bottomLeft, bottomRight] = coordinates;

      const topSide = topRight[0] === topLeft[0];  // Top side should be horizontal
      const bottomSide = bottomRight[0] === bottomLeft[0]; // Bottom side should be horizontal
      const leftSide = topLeft[1] === bottomLeft[1]; // Left side should be vertical
      const rightSide = topRight[1] === bottomRight[1]; // Right side should be vertical

      return topSide && bottomSide && leftSide && rightSide;
    }

    function removeRedundantPoints(coordinates) {
      // Convert each point to a string format (e.g., "x,y") and use a Set to remove duplicates
      const uniqueSet = new Set(coordinates.map(point => `${point[0]},${point[1]}`));
      // Convert the Set back to an array
      return Array.from(uniqueSet).map(point => point.split(',').map(Number));
    }

    function createEmptyEntry() {
      return {
        leafMap: undefined,
        leafMapInitialized: false,
        drawTools: {
          allowMarker: false,
          allowRectangle: false,
          allowPolygon: false,
        },
        markerA: undefined,
        markerB: undefined,
        selection: undefined,
        values: {},
        widgetVars: {},
      };
    }

    let editView = {
      // Update position of markers and selection rectangle using the stored values
      updateMap: function(key) {
        let data = editMapsData.get(key);
        updateMapCoordinates(data, data.values[TEXT_AREA_COORDINATES])
        data.polygonLayer.eachLayer(function (shape) {
          shape.enableEdit();
        });
      },

      prepare: function(key) {
        if (editMapsData.has(key)) {
          // remove existing map on partial page update
          editMapsData.get(key).leafMap.remove();
        }
        var options = createEmptyEntry();
        options.drawTools.allowMarker = true
        options.drawTools.allowRectangle = true
        options.drawTools.allowPolygon = true
        editMapsData.set(key, options);
      },

      remove: function(key) {
        editMapsData.get(key).leafMap.remove();
        editMapsData.delete(key);
      },

      putValue: function(key, field, value) {
        putValue(editMapsData, key, field, value);
      },

      putWidgetVar: function(key, field, widgetVar) {
        let data = editMapsData.get(key);
        data.widgetVars[field] = widgetVar;
      },

      storeCoordinates: function(key, value) {
        let data = editMapsData.get(key);
        data.values[TEXT_AREA_COORDINATES] = value;
      },

      initializeAll: function(keyPrefix) {
        initializeAll(editMapsData, keyPrefix, initializeMapInView.bind(this));
      }
    }

    // SearchView – methods & data for advanced search form

    let searchMapsData = new Map();

    let searchView = {

      updateMap: function (key) {
        let data = searchMapsData.get(key);
        let coordinates = parseCoordinates(data.values[TEXT_AREA_COORDINATES]);
        if (isRectangleAxisAligned(coordinates)) {
          updateMapCoordinates(data, data.values[TEXT_AREA_COORDINATES]);
          data.polygonLayer.eachLayer(function (shape) {
            shape.enableEdit();
          });
        } else {
          data.polygonLayer.clearLayers();
        }
      },

      prepare: function(key) {
        if (key && searchMapsData.has(key)) {
          return;
        }

        let options = createEmptyEntry();
        options.drawTools.allowRectangle = true;
        options.advancedSearch = true;
        searchMapsData.set(key, options);
      },

      storeCoordinates: function(key, value) {
        let data = searchMapsData.get(key);
        data.values[TEXT_AREA_COORDINATES] = value;
      },

      putWidgetVar: function (key, field, widgetVar) {
        let data = searchMapsData.get(key);
        data.widgetVars[field] = widgetVar;
      },

      initializeAll: function(keyPrefix) {
        initializeAll(searchMapsData, keyPrefix, initializeMapInView.bind(this));
      },

      removeAll: function() {
        for (const data of searchMapsData.values()) {
          data.leafMap.remove();
        }
        searchMapsData.clear();
      }
    }

     // SearchResults – methods & data for search results

    let searchResultsData = new Map();

    function initializeMapSearchResults(key, data) {
      const mapOptions = Object.assign({}, INIT_MAP_OPTS, {
        zoom: 1,
        minZoom: 1
      });
      data.leafMap = L.map(key, mapOptions);
      let map = data.leafMap;
      data.polygonLayer = L.layerGroup().addTo(map);
      L.tileLayer(TILE_LAYER_URL, { maxZoom: MAX_ZOOM, attribution: TILE_LAYER_COPYRIGHT }).addTo(map);
    }

    // call invalidateSize to re-render leaflet map
    function render(key, data) {
      let map = data.leafMap
      map.invalidateSize();
    }

    function onMarkerMouseOver(evt ,data, dataset) {
      data.polygonLayer.clearLayers();
      cords = dataset.coordinates.map(a => [a.latitude, a.longitude] )

      if (cords.length == 2) {
          L.polyline(cords).addTo(data.polygonLayer)
      } else if (cords.length >= 3) {
          L.polygon(cords).addTo(data.polygonLayer);
      }
    }

    let searchResults = {
      prepare: function (key) {
        searchResultsData.set(key, {
          leafMap: undefined,
          leafMapInitialized: false,
          markerDialogTemplate: 'mapMarkerDialogTemplate'
        });
      },
      initializeAll: function(keyPrefix) {
        initializeAll(searchResultsData, keyPrefix, initializeMapSearchResults.bind(this));
      },
      render: function(key) {
        let mapData = searchResultsData.get(key);
        render(key, mapData);
      },
      translation: function (key, fieldName, translation) {
        let mapData = searchResultsData.get(key);
        mapData[fieldName] = translation;
      },
      // value: representing geo marker coordinates with name
      // eq {"name": "test", "marker": {"latitude": 1, "longitude": 2}}
      addClusteredMarkers: function (key, field, value) {
        if (!value || value.length === 0) {
            return;
        }

        putValue(searchResultsData, key, field, value);
        let mapData = searchResultsData.get(key);
        var markers = L.markerClusterGroup();

        for (const dataset of value) {
            var marker = L.marker([dataset.marker.latitude, dataset.marker.longitude])
                .bindPopup(renderTemplate(mapData.markerDialogTemplate, dataset))
                .on('mouseover', function (evt) {
                    onMarkerMouseOver(evt, mapData, dataset);
                 })
                .on('mouseout', function (evt) {
                    mapData.polygonLayer.clearLayers();
                });
            markers.addLayer(marker);
        }

        mapData.leafMap.addLayer(markers);
      }
    }

    return {
      MetadataView: metadataView,
      EditView: editView,
      SearchView: searchView,
      SearchResults: searchResults,
    };
  }


  function initControlledVocabEnhancedInput() {
    // Helper function to find a scrollable element by traversing the DOM tree
    function findScrollableElement(element) {
      // Traverse the DOM tree and check if the element is scrollable
      while (element) {
        var e = $(element);
        if (   e.css('overflow') == 'scroll'
            || e.css('overflow') == 'auto'
            || e.css('overflowY') == 'scroll'
            || e.css('overflowY') == 'auto'
            || e.css('height') != 'none'
            || e.css('max-height') != 'none'
        ) {
          return element;
        } else {
          element = element.firstElementChild;  // Go deeper into the tree
        }
      }
      return null;  // No scrollable element found
    }
    function scrollToBottomOfScrollable(autoCompleteWidget) {
      if (autoCompleteWidget) {
        // Use a timeout to ensure the panel is visible and rendered
        setTimeout(function() {
          // Start from the widget's panel and traverse down the tree
          var panel = PF(autoCompleteWidget).panel;
          var children =  panel[0];
          if (panel) {
            var scrollableElement = findScrollableElement(children);
            if (scrollableElement) {
              // Scroll the element to the bottom
              scrollableElement.scrollTop = scrollableElement.scrollHeight;
            }
          }
        }, 500);
      }
    }
    let reopenAutocompleteSuggestions = {
      prepare: function (widgetVar) {
        var widget = PF(widgetVar);
        if (widget) {
          widget.searchWithDropdown();
          scrollToBottomOfScrollable(widgetVar);
        }
      }
    }
    return {
      EnhancedSelectView: reopenAutocompleteSuggestions,
    };
  }

  return {
    Geo: initGeo(),
    EnhancedSelect: initControlledVocabEnhancedInput()
  };

}
