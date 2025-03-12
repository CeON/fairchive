function initDvJS() {
  
  function initGeo() {
    // Coordinates mapping in GEOBOX datafield
    const X1 = 'W';
    const Y1 = 'S';
    const X2 = 'E';
    const Y2 = 'N';
    const TEXT_AREA_COORDINATES = 'textAreaCoordinates'

    const RECT_COLOR = '#e3276f';
    const MAX_ZOOM = 19;
    const INIT_MAP_OPTS = { center: [52.1145028, 19.4235611], zoom: 4 };

    const TILE_LAYER_URL = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
    const TILE_LAYER_COPYRIGHT = '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>';

    // Checks whether geobox is properly defined
    function isValidGeobox(values) {
      return values && Object.keys(values).length === 4 && values[Y2] >= values[Y1];
    }

    // Checks whether we should wrap geobox around antimeridian
    function shouldWrapGeobox(values) {
      return values[X1] > values[X2];
    }

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

    // MetadataView – methods & data for Dataset/Metadata tab

    // Draw map with geobox rectangle (must be called only when the target div is visible!)
    function initializeMapInMetadataView(key, data) {
      data.leafMap = L.map(key);
      let leafMap = data.leafMap;
      leafMap.invalidateSize();
      let values = data.values;
      if (!isValidGeobox(values)) {
        return;
      }
      L.tileLayer(TILE_LAYER_URL, { maxZoom: MAX_ZOOM, attribution: TILE_LAYER_COPYRIGHT }).addTo(leafMap);
      let bounds = L.latLngBounds([
        [values[Y1], values[X1]],
        [values[Y2], values[X2] + (shouldWrapGeobox(values) ? 1 : 0) * 360.0]
      ]);
      ((values[X1] === values[X2] && values[Y1] === values[Y2])
        ? L.marker([values[Y1], values[X1]]).addTo(leafMap)
        : L.rectangle(bounds, { color: RECT_COLOR, weight: 1 }))
        .addTo(leafMap);
      leafMap.fitBounds(bounds.pad(0.1));
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

    function updateInputs(data) {
      let bounds = data.selection.getBounds();
      let sw = bounds.getSouthWest().wrap();
      updateInputAndValue(data, X1, sw.lng);
      updateInputAndValue(data, Y1, sw.lat);
      let ne = bounds.getNorthEast().wrap();
      updateInputAndValue(data, X2, ne.lng);
      updateInputAndValue(data, Y2, ne.lat);
    }

    function updateInputAndValue(data, coord, value) {
      if (!PF(data.widgetVars[coord])) {
        throw new Error('Missing dataset field supporting rectangles edit: ' +
        'westLongitude, eastLongitude, northLongitude, southLongitude');
      }

      PF(data.widgetVars[coord]).jq.val(value.toFixed(5));
      data.values[coord] = Number(value);
    }

    function onMapClicked(evt, data) {
      if (!data.markerA) {
        data.markerA = addMarker(evt.latlng, data);
      } else if (!data.markerB) {
        data.markerB = addMarker(evt.latlng, data);
        data.selection = addSelection(data);
        updateInputs(data);
        centerMap(data, data.selection.getBounds());
        data.leafMap.off('click'); // Unregister this handler after adding second marker
      } else {
        data.leafMap.off('click'); // Unregister this handler if all markers are already present
      }
    }

    function addMarker(latLng, data) {
      return L.marker(latLng, { draggable: true, autoPan: true, autoPanPadding: L.point(10, 10), autoPanSpeed: 1 })
              .addTo(data.leafMap)
              .on('dragend', function() { onMarkerDragged(data) });
    }

    function addSelection(data) {
      return L.rectangle([data.markerA.getLatLng(), data.markerB.getLatLng()], 
                          { color: RECT_COLOR, weight: 1 })
              .addTo(data.leafMap);
    }

    function initializeMapInView(key, data) {
      if (data.polygonSupport) {
        INIT_MAP_OPTS.editable = true;
      }
      data.leafMap = L.map(key, INIT_MAP_OPTS);
      let map = data.leafMap;
      map.invalidateSize();
      if (data.polygonSupport) {
        data.polygonLayer = L.layerGroup().addTo(map);
        map.on('editable:drawing:commit', function (evt) { createdShape(evt ,data); })
        map.on('editable:edited', function (e) {
          updateTextArea(e.layer, data.widgetVars);
        });
        map.on('editable:vertex:new', function (e) {
          updateTextArea(e.layer, data.widgetVars);
        });
        map.on('editable:vertex:deleted', function (e) {
          updateTextArea(e.layer, data.widgetVars);
        });

        createBaseEditControls()
        createNewMarkerControl(map, data.polygonLayer)
        createNewPolygonControl(map, data.polygonLayer)
        createNewRectangleControl(map, data.polygonLayer)
      } else {
        map.on('click', function (evt) { onMapClicked(evt, data) });
      }

      L.tileLayer(TILE_LAYER_URL, { maxZoom: MAX_ZOOM, attribution: TILE_LAYER_COPYRIGHT }).addTo(map);
      this.updateMap(key);
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
            map.editTools.startPolygon();
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

        updateTextArea(layer, data.widgetVars);
    }

    function updateTextArea(layer, widgetVars) {
      if (!PF(widgetVars['polygonGeo'])) {
        throw new Error('Missing dataset field supporting polygon edit: polygonGeo');
      }

      const geometry = layer.toGeoJSON().geometry.coordinates;

      const isPolygon = Array.isArray(geometry[0]);
      if (isPolygon) {
        result = geometry[0].map(row => row.join(" ")).join("\n");
      } else {
        result = geometry.join(" ");
      }
      PF(widgetVars['polygonGeo']).jq.val(result);
    }

    function updateEditableMap(dataMap, key) {
      let data = dataMap.get(key);
      if (!data || !isValidGeobox(data.values)) {
        return;
      }
      let wrap = shouldWrapGeobox(data.values);
      data.markerA = !data.markerA
        ? addMarker([data.values[Y1], data.values[X1]], data)
        : data.markerA.setLatLng([data.values[Y1], data.values[X1]]);
      data.markerB = !data.markerB
        ? addMarker([data.values[Y2], data.values[X2] + wrap * 360.0], data)
        : data.markerB.setLatLng([data.values[Y2], data.values[X2] + wrap * 360.0]);
      data.selection = !data.selection
        ? addSelection(data)
        : data.selection.setBounds([data.markerA.getLatLng(), data.markerB.getLatLng()]);
      centerMap(data, data.selection.getBounds());
    }

    // Draw on map: marker, line, polygon using list of coordinates
    // coordinates - excepted format, each line represent pair of longitude, latitude
    // e.q.
    // 1.112 41.12
    // 2.12 15.21
    function updateMapCoordinates(key, coordinates) {
      if (!coordinates || coordinates.trim() === '') {
        return;
      }

      var data = editMapsData.get(key);
      if (!data.polygonLayer) {
        return;
      }

      var cords = coordinates
                .trim() // Remove extra spaces/newlines
                .split("\n") // Split by new lines
                .map(line => line.trim().split(/\s+/).map(Number))
                .map(coord => [coord[1], coord[0]]); // Reverse the order (longitude, latitude) -> (latitude, longitude)
      // validation
      const hasTwoNumericPoints = cords.every(cord =>
        Array.isArray(cord) &&
        cord.length === 2 &&
        cord.every(point => !isNaN(point))
      );
      if (cords.length == 0 || !hasTwoNumericPoints) {
          return;
      }

      data.polygonLayer.clearLayers();
      var shape;
      if (cords.length == 1) {
          shape = L.marker(cords[0]).addTo(data.polygonLayer)
      } else if (cords.length == 2) {
          shape = L.polyline(cords).addTo(data.polygonLayer)
      } else if (cords.length >= 3) {
          shape = L.polygon(cords).addTo(data.polygonLayer);
      }

      const bounds = L.latLngBounds(cords);
      centerMap(data, bounds);
      shape.enableEdit();
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

    function createEmptyEntry() {
      return {
        leafMap: undefined,
        leafMapInitialized: false,
        polygonSupport: false,
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
        if (data.polygonSupport) {
          updateMapCoordinates(key, data.values[TEXT_AREA_COORDINATES])
        } else {
          updateEditableMap(editMapsData, key);
        }
      },

      prepare: function(key) {
        if (editMapsData.has(key)) {
          // remove existing map on partial page update
          editMapsData.get(key).leafMap.remove();
        }
        editMapsData.set(key, createEmptyEntry());
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
        if (data.polygonSupport) {
          data.values[TEXT_AREA_COORDINATES] = value;
        }
      },
      // Enable advanced map edit mode. Allow to draw markers, rectangles, polygons
      enablePolygonSupport: function(key) {
        let data = editMapsData.get(key);
        data.polygonSupport = true;
      },

      initializeAll: function(keyPrefix) {
        initializeAll(editMapsData, keyPrefix, initializeMapInView.bind(this));
      }
    }

    // SearchView – methods & data for advanced search form

    let searchMapsData = new Map();

    let searchView = {
      // As the parent fields on advanced search form are not used to render 
      // their subfields, we have to count coordinate fields with the same key
      canCreateMap: function(key) {
        let vars = searchMapsData.get(key).widgetVars;
        return key && Object.keys(vars).length === 4;
      },

      updateMap: function (key) {
        updateEditableMap(searchMapsData, key);
      },

      prepare: function(key) {
        if (key && searchMapsData.has(key)) {
          return;
        }
        searchMapsData.set(key, createEmptyEntry());
      },

      putValue: function (key, field, value) {
        putValue(searchMapsData, key, field, value);
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
      data.leafMap = L.map(key, INIT_MAP_OPTS);
      let map = data.leafMap;
      L.tileLayer(TILE_LAYER_URL, { maxZoom: MAX_ZOOM, attribution: TILE_LAYER_COPYRIGHT }).addTo(map);
    }

    // call invalidateSize to re-render leaflet map
    function render(key, data) {
      let map = data.leafMap
      map.invalidateSize();
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
                    .bindPopup(renderTemplate(mapData.markerDialogTemplate, dataset));
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
