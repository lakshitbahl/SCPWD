<?php
	include("fusioncharts.php");
?>
<html>
	<head>
		<title>spMonitor Test</title>
		<script type="text/javascript" src="fusioncharts/fusioncharts.js"></script>
		<script type="text/javascript" src="fusioncharts/themes/fusioncharts.theme.zune.js"></script>
		<link rel="stylesheet" href="jq/jquery-ui.min.css">
        <script src="jq/external/jquery/jquery.js"></script>
        <script src="jq/jquery-ui.min.js"></script>
		<script type="text/javascript">
			$(document).ready(function(e){
				e.preventDefault();
				var minDay="<?php echo $datePickerMin; ?>";
				$("#date").datepicker({
					minDate: minDay,
					maxDate: 0,
					dateFormat: "yy-MM-dd"
				});
			});
		</script>
		<!--
		.scroll{
			width:auto;
			height:10px;
			overflow:auto;
		}
		-->
	</head>

	<body bgcolor="#000000">
	<font color="white">
		<?php
			$daySelected = $_GET['day'];
			$plotType = $_GET['type'];
			/*
			 * Connect to SQLite database returning results as JSON
			 * START SQLite Section
			 */
			// Specify your sqlite database name and path
			$dir = 'sqlite:/mnt/sda1/s.db';
			// Instantiate PDO connection object and failure msg
			$dbh = new PDO($dir) or die("cannot open database");

			$yearsAvail = array();
			$recordsAvail;

			// Start drawing the table
			echo "<div style=\"overflow: auto; max-height: 100px; width: 300px; float: top; background: lightgrey; padding-left: 10px; margin: 30px\">";

			// First available date
			$firstYear = "";
			$firstMonth = "";
			$firstDay = "";
			// Define your SQL statement to get available years
			$query = "SELECT DISTINCT SUBSTR(d,1,2) y FROM s";
			$sth = $dbh->query($query);
			while($row = $sth->fetch(PDO::FETCH_ASSOC)) {
				$yearsAvail[] = $row;
			};
			for ($year=0; $year<count($yearsAvail); $year++) {
				$monthsAvail = array();
				// Define your SQL statement to get available months per year
				$query = "SELECT DISTINCT SUBSTR(d,4,2) m FROM s WHERE d LIKE '" . $yearsAvail[$year]['y'] . "%'";
				$sth = $dbh->query($query);
				while($row = $sth->fetch(PDO::FETCH_ASSOC)) {
					$monthsAvail[] = $row;
				}
				for ($month=0; $month<count($monthsAvail); $month++) {
					$daysAvail = array();
					// Define your SQL statement to get available days per month per year
					$query = "SELECT DISTINCT SUBSTR(d,7,2) d FROM s WHERE d LIKE '" . $yearsAvail[$year]['y'] .
					 "-" . $monthsAvail[$month]['m'] . "%'";
					$sth = $dbh->query($query);
					$day = 0;
					while($row = $sth->fetch(PDO::FETCH_ASSOC)) {
						$daysAvail[] = $row;
						$fileName=$yearsAvail[$year][y]."-".$monthsAvail[$month][m]."-".$daysAvail[$day][d];
						$recordsAvail[$day] = $fileName;
						$day++;
						echo "<table><tr><td><a href='index.php?day="
								.$fileName
								."&type=MSArea'>"
								.$fileName." filled </a></td><td style=\"padding-left: 40px\"><a href='index.php?day=".$fileName
								."&type=zoomline'>"
								.$fileName
								." zoomable </a></td></tr></table>";
					}
				}
			}
			echo "</div>";
			$firstYear=$yearsAvail[0][y];
			$firstMonth=$monthsAvail[0][m];
			$firstDay=$daysAvail[0][d];
			print_r ("First Year: $firstYear</br>");
			print_r ("First Month: $firstMonth</br>");
			print_r ("First Day: $firstDay</br>");
			$datePickerMin = "$firstYear-$firstMonth-$firstDay";
            print_r ("First Date: $datePickerMin</br>");
            echo "<SCRIPT LANGUAGE='javascript'>add dd_Employee('".$id1."','".$fname1."',".$sal1.");</SCRIPT>";

			if (!empty($daySelected)) {
               	$timeStamps = array();
               	$solarRecords = array();
               	$consRecords = array();
               	$lightRecords = array();
				// Define your SQL statement to get time stamps
                $query = "SELECT * FROM s WHERE d LIKE '" . $daySelected . "%' ORDER BY d";
				$sth = $dbh->query($query);
				$index = 0;
				while($row = $sth->fetch(PDO::FETCH_ASSOC)) {
                	$json[] = $row;
                	array_push($timeStamps,$json[$index][d]);
                	array_push($solarRecords,$json[$index][s]);
                	array_push($consRecords,$json[$index][c]);
                	array_push($lightRecords,$json[$index][l]);
                	$index++;
				}
				$jsonData = "{\"chart\": {
					\"caption\": \"Solar Panel Monitor\",
					\"subCaption\": \"20".$daySelected."\",
					\"xAxisName\": \"Time\",
					\"yAxisName\": \"Watt\",
					\"connectNullData\": \"0\",
					\"adjustDiv\": \"0\",
					\"captionFontSize\": \"14\",
					\"subcaptionFontSize\": \"14\",
					\"subcaptionFontBold\": \"0\",
					\"paletteColors\": \"#FF8C00,#00008B\",
					\"bgcolor\": \"#FFFFFF\",
					\"canvasBgColor\": \"#AAAAAA\",
					\"legendItemFontSize\": \"20\",
					\"legendBgColor\": \"#DDDDDD\",
					\"showBorder\": \"0\",
					\"showShadow\": \"0\",
					\"plotFillAlpha\": \"60\",
					\"showCanvasBorder\": \"0\",
					\"usePlotGradientColor\": \"0\",
					\"legendBorderAlpha\": \"0\",
					\"legendShadow\": \"0\",
					\"showAxisLines\": \"0\",
					\"showAlternateHGridColor\": \"0\",
					\"divlineThickness\": \"1\",
					\"divLineIsDashed\": \"1\",
					\"divLineDashLen\": \"1\",
					\"divLineGapLen\": \"1\",
					\"showPeakData \": \"0\",
					\"maxPeakDataLimit  \": \"0\",
					\"minPeakDataLimit  \": \"0\",
					\"showValues\": \"0\"
				},";

				$jsonData = $jsonData . "\"categories\": [ { \"category\": [";

				for ($index=0; $index<count($timeStamps); $index++) {
					$jsonData = $jsonData . "{ \"label\": \"" . substr($timeStamps[$index],9,5) . "\" },";
				}

				$jsonData = $jsonData . " ] } ],";
				$jsonData = $jsonData . "\"dataset\": [ { \"seriesname\": \"Solar\", \"data\": [";

				for ($index=0; $index<count($solarRecords); $index++) {
					$jsonData = $jsonData . "{ \"value\": \"" . $solarRecords[$index] . "\" },";
				}

				$jsonData = $jsonData . " ] },";
				$jsonData = $jsonData . "{ \"seriesname\": \"Consumption\", \"data\": [";

				for ($index=0; $index<count($consRecords); $index++) {
					$jsonData = $jsonData . "{ \"value\": \"" . $consRecords[$index] . "\" },";
				}

				$jsonData = $jsonData . " ] } ] }";

				if (!($plotType=="MSArea") && !($plotType=="zoomline")) {
					$plotType = "MSArea";
				}
				$columnChart = new FusionCharts($plotType, "spm1" , "100%", 500, "chartContainer", "json", $jsonData);

				// Render the chart

				$columnChart->render();
			}
		?>

		<h2>Select day to show</h2>

		<form method="post" action="index.php?day=#date".$date id="selDate">
			Select day : <input type="text" name="date" id="date"/>
			<input type="submit" name="submit" value="Submit">
		</form>

		<div id="chartContainer">spMonitor plot from database will load here!</div>
	</font>
	</body>
</html>
