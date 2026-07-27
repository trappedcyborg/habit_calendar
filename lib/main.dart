import 'package:flutter/material.dart';
import 'package:home_widget/home_widget.dart';

Future<void> updateHomeWidget(int currentDay, int totalDays) async {
  // 1. Save data shared across native widget providers
  await HomeWidget.saveWidgetData<int>('currentDay', currentDay);
  await HomeWidget.saveWidgetData<int>('totalDays', totalDays);

  // 2. Update Mini Grid Calendar (showing "2026")
  await HomeWidget.updateWidget(
    name: 'HabitCalendarWidgetProvider',
    androidName: 'HabitCalendarWidgetProvider',
  );

  // 3. Update Detailed Grid Calendar (showing "Mon, 27 Jul 2026")
  await HomeWidget.updateWidget(
    name: 'DateGridWidgetProvider',
    androidName: 'DateGridWidgetProvider',
  );

  // 4. Update Progress Tracker (circular %)
  await HomeWidget.updateWidget(
    name: 'ProgressWidgetProvider',
    androidName: 'ProgressWidgetProvider',
  );
}

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const GridCalendarApp());
}

class GridCalendarApp extends StatelessWidget {
  const GridCalendarApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Grid Calendar View',
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark().copyWith(
        scaffoldBackgroundColor: Colors.black,
        // Set SFPro globally for all Flutter text widgets
        textTheme: ThemeData.dark().textTheme.apply(
          fontFamily: 'SFPro',
        ),
        primaryTextTheme: ThemeData.dark().primaryTextTheme.apply(
          fontFamily: 'SFPro',
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF1E1E1E),
          elevation: 0,
          centerTitle: true,
          titleTextStyle: TextStyle(
            fontFamily: 'SFPro',
            color: Colors.white,
            fontSize: 20,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
      home: const HomeScreen(),
    );
  }
}

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  late int currentDay;
  late int totalDays;
  late double percentCompleted;

  @override
  void initState() {
    super.initState();
    _calculateDateData();
  }

  void _calculateDateData() {
    final now = DateTime.now();
    final year = now.year;
    final isLeap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    totalDays = isLeap ? 366 : 365;

    final diff = now.difference(DateTime(year, 1, 1));
    currentDay = diff.inDays + 1;

    percentCompleted = (currentDay / totalDays) * 100;

    // Sync data to native Android HomeWidgets
    updateHomeWidget(currentDay, totalDays);
  }

  @override
  Widget build(BuildContext context) {
    final now = DateTime.now();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Grid Calendar View'),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16.0),
          child: Column(
            children: [
              const SizedBox(height: 20),

              // Header: "Day 207 of 365"
              RichText(
                text: TextSpan(
                  style: const TextStyle(
                    fontFamily: 'SFPro',
                    fontSize: 28,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
                  children: [
                    const TextSpan(text: 'Day '),
                    TextSpan(
                      text: '$currentDay',
                      style: const TextStyle(
                        fontFamily: 'SFPro',
                        color: Color(0xFFFF5252),
                      ),
                    ),
                    TextSpan(text: ' of $totalDays'),
                  ],
                ),
              ),

              const SizedBox(height: 6),

              // Subheader: "57% of 2026 completed"
              Text(
                '${percentCompleted.toStringAsFixed(0)}% of ${now.year} completed',
                style: const TextStyle(
                  fontFamily: 'SFPro',
                  color: Colors.grey,
                  fontSize: 16,
                ),
              ),

              const SizedBox(height: 24),

              // Exactly 15 Columns matching your screenshot
              Expanded(
                child: GridView.builder(
                  physics: const BouncingScrollPhysics(),
                  gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                    crossAxisCount: 15,
                    crossAxisSpacing: 5,
                    mainAxisSpacing: 5,
                    childAspectRatio: 1.0,
                  ),
                  itemCount: totalDays,
                  itemBuilder: (context, index) {
                    final dayNumber = index + 1;
                    Color boxColor;

                    if (dayNumber == currentDay) {
                      boxColor = const Color(0xFFFF5252);
                    } else if (dayNumber < currentDay) {
                      boxColor = Colors.white;
                    } else {
                      boxColor = const Color(0xFF2A2A2A);
                    }

                    return Container(
                      decoration: BoxDecoration(
                        color: boxColor,
                        borderRadius: BorderRadius.circular(5.0),
                      ),
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class GridPainter extends CustomPainter {
  final int totalDays;
  final int currentDay;

  GridPainter({
    required this.totalDays,
    required this.currentDay,
  });

  @override
  void paint(Canvas canvas, Size size) {
    const int cols = 15;
    final double spacing = size.width * 0.015;
    final double boxWidth = (size.width - (spacing * (cols - 1))) / cols;
    final double boxHeight = boxWidth;

    final Paint paintPassed = Paint()
      ..color = Colors.white
      ..style = PaintingStyle.fill;

    final Paint paintToday = Paint()
      ..color = const Color(0xFFFF5252)
      ..style = PaintingStyle.fill;

    final Paint paintFuture = Paint()
      ..color = const Color(0xFF2A2A2A)
      ..style = PaintingStyle.fill;

    for (int i = 0; i < totalDays; i++) {
      final int dayNumber = i + 1;
      final int row = i ~/ cols;
      final int col = i % cols;

      final double left = col * (boxWidth + spacing);
      final double top = row * (boxHeight + spacing);

      final Rect rect = Rect.fromLTWH(left, top, boxWidth, boxHeight);
      final RRect rrect = RRect.fromRectAndRadius(
        rect,
        Radius.circular(boxWidth * 0.2),
      );

      Paint activePaint;
      if (dayNumber == currentDay) {
        activePaint = paintToday;
      } else if (dayNumber < currentDay) {
        activePaint = paintPassed;
      } else {
        activePaint = paintFuture;
      }

      canvas.drawRRect(rrect, activePaint);
    }
  }

  @override
  bool shouldRepaint(covariant GridPainter oldDelegate) {
    return oldDelegate.currentDay != currentDay || oldDelegate.totalDays != totalDays;
  }
}