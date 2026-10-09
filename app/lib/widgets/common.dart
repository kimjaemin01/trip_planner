import 'package:flutter/material.dart';

/// 추천도 별 (0~5, 0.5 단위)
class StarRating extends StatelessWidget {
  final double score;
  final double size;

  const StarRating({super.key, required this.score, this.size = 16});

  @override
  Widget build(BuildContext context) {
    final color = Colors.amber.shade700;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: List.generate(5, (i) {
        final value = score - i;
        final icon = value >= 0.75
            ? Icons.star_rounded
            : value >= 0.25
                ? Icons.star_half_rounded
                : Icons.star_border_rounded;
        return Icon(icon, size: size, color: color);
      }),
    );
  }
}

/// 화면 하단 고정 버튼
class BottomActionButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;

  const BottomActionButton({super.key, required this.label, this.onPressed});

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      minimum: const EdgeInsets.fromLTRB(16, 8, 16, 16),
      child: SizedBox(
        width: double.infinity,
        height: 52,
        child: FilledButton(
          onPressed: onPressed,
          child: Text(label, style: const TextStyle(fontSize: 16)),
        ),
      ),
    );
  }
}

/// 불러오기 실패 화면
class LoadError extends StatelessWidget {
  final Object error;
  final VoidCallback onRetry;

  const LoadError({super.key, required this.error, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off_rounded, size: 48),
            const SizedBox(height: 12),
            const Text('서버에 연결할 수 없어요'),
            const SizedBox(height: 4),
            Text('$error',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodySmall),
            const SizedBox(height: 16),
            OutlinedButton(onPressed: onRetry, child: const Text('다시 시도')),
          ],
        ),
      ),
    );
  }
}

/// 단계 안내 제목
class StepHeader extends StatelessWidget {
  final String step;
  final String title;
  final String? subtitle;

  const StepHeader(
      {super.key, required this.step, required this.title, this.subtitle});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(step,
              style: theme.textTheme.labelLarge
                  ?.copyWith(color: theme.colorScheme.primary)),
          const SizedBox(height: 4),
          Text(title,
              style: theme.textTheme.headlineSmall
                  ?.copyWith(fontWeight: FontWeight.bold)),
          if (subtitle != null) ...[
            const SizedBox(height: 6),
            Text(subtitle!,
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
          ],
        ],
      ),
    );
  }
}

String formatDays(double d) =>
    d == d.roundToDouble() ? '${d.toInt()}일' : '${d.toStringAsFixed(1)}일';

String formatMinutes(int min) {
  final h = min ~/ 60;
  final m = min % 60;
  if (h == 0) return '$m분';
  if (m == 0) return '$h시간';
  return '$h시간 $m분';
}
